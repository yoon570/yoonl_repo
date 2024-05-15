/*
 * cush - the customizable shell.
 *
 * Developed by Godmar Back for CS 3214 Summer 2020 
 * Virginia Tech.  Augmented to use posix_spawn in Fall 2021.
 */
#define _GNU_SOURCE    1
#include <stdio.h>
#include <readline/readline.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <termios.h>
#include <sys/wait.h>
#include <assert.h>
#include <readline/history.h>
#include <fcntl.h>

/* Since the handed out code contains a number of unused functions. */
#pragma GCC diagnostic ignored "-Wunused-function"

#include "termstate_management.h"
#include "signal_support.h"
#include "shell-ast.h"
#include "utils.h"
#include "spawn.h"
extern char **environ;

static void handle_child_status(pid_t pid, int status);

static void
usage(char *progname)
{
    printf("Usage: %s -h\n"
        " -h            print this help\n",
        progname);

    exit(EXIT_SUCCESS);
}

/* Build a prompt */
static char *
build_prompt(void)
{
    return strdup("cush> ");
}

enum job_status {
    FOREGROUND,     /* job is running in foreground.  Only one job can be
                       in the foreground state. */
    BACKGROUND,     /* job is running in background */
    STOPPED,        /* job is stopped via SIGSTOP */
    NEEDSTERMINAL,  /* job is stopped because it was a background job
                       and requires exclusive terminal access */
};

struct job {
    struct list_elem elem;   /* Link element for jobs list. */
    struct ast_pipeline *pipe;  /* The pipeline of commands this job represents */
    int     jid;             /* Job id. */
    enum job_status status;  /* Job status. */ 
    int  num_processes_alive;   /* The number of processes that we know to be alive */
    struct termios saved_tty_state;  /* The state of the terminal when this job was 
                                        stopped after having been in foreground */
    pid_t pid;
    pid_t *pids;
    int num_cmds;
    int changed_tty;
    /* Add additional fields here if needed. */
};

/* Utility functions for job list management.
 * We use 2 data structures: 
 * (a) an array jid2job to quickly find a job based on its id
 * (b) a linked list to support iteration
 */
#define MAXJOBS (1<<16)
static struct list job_list;

static struct job * jid2job[MAXJOBS];

/* Return job corresponding to jid */
static struct job * 
get_job_from_jid(int jid)
{
    if (jid > 0 && jid < MAXJOBS && jid2job[jid] != NULL)
        return jid2job[jid];

    return NULL;
}

/* Add a new job to the job list */
static struct job *
add_job(struct ast_pipeline *pipe)
{
    struct job * job = malloc(sizeof *job);
    job->pipe = pipe;
    job->num_processes_alive = 0;
    list_push_back(&job_list, &job->elem);
    for (int i = 1; i < MAXJOBS; i++) {
        if (jid2job[i] == NULL) {
            jid2job[i] = job;
            job->jid = i;
            return job;
        }
    }
    fprintf(stderr, "Maximum number of jobs exceeded\n");
    abort();
    return NULL;
}

/* Delete a job.
 * This should be called only when all processes that were
 * forked for this job are known to have terminated.
 */
static void
delete_job(struct job *job)
{
    int jid = job->jid;
    assert(jid != -1);
    jid2job[jid]->jid = -1;
    jid2job[jid] = NULL;
    ast_pipeline_free(job->pipe);
    free(job);
}

static const char *
get_status(enum job_status status)
{
    switch (status) {
    case FOREGROUND:
        return "Foreground";
    case BACKGROUND:
        return "Running";
    case STOPPED:
        return "Stopped";
    case NEEDSTERMINAL:
        return "Stopped (tty)";
    default:
        return "Unknown";
    }
}

/* Print the command line that belongs to one job. */
static void
print_cmdline(struct ast_pipeline *pipeline)
{
    struct list_elem * e = list_begin (&pipeline->commands); 
    for (; e != list_end (&pipeline->commands); e = list_next(e)) {
        struct ast_command *cmd = list_entry(e, struct ast_command, elem);
        if (e != list_begin(&pipeline->commands))
            printf("| ");
        char **p = cmd->argv;
        printf("%s", *p++);
        while (*p)
            printf(" %s", *p++);
    }
}

/* Print a job */
static void
print_job(struct job *job)
{
    printf("[%d]\t%s\t\t(", job->jid, get_status(job->status));
    print_cmdline(job->pipe);
    printf(")\n");
}

/*
 * Suggested SIGCHLD handler.
 *
 * Call waitpid() to learn about any child processes that
 * have exited or changed status (been stopped, needed the
 * terminal, etc.)
 * Just record the information by updating the job list
 * data structures.  Since the call may be spurious (e.g.
 * an already pending SIGCHLD is delivered even though
 * a foreground process was already reaped), ignore when
 * waitpid returns -1.
 * Use a loop with WNOHANG since only a single SIGCHLD 
 * signal may be delivered for multiple children that have 
 * exited. All of them need to be reaped.
 */
static void
sigchld_handler(int sig, siginfo_t *info, void *_ctxt)
{
    pid_t child;
    int status;

    assert(sig == SIGCHLD);

    while ((child = waitpid(-1, &status, WUNTRACED|WNOHANG)) > 0) {
        handle_child_status(child, status);
    }
}

/* Wait for all processes in this job to complete, or for
 * the job no longer to be in the foreground.
 * You should call this function from a) where you wait for
 * jobs started without the &; and b) where you implement the
 * 'fg' command.
 * 
 * Implement handle_child_status such that it records the 
 * information obtained from waitpid() for pid 'child.'
 *
 * If a process exited, it must find the job to which it
 * belongs and decrement num_processes_alive.
 *
 * However, note that it is not safe to call delete_job
 * in handle_child_status because wait_for_job assumes that
 * even jobs with no more num_processes_alive haven't been
 * deallocated.  You should postpone deleting completed
 * jobs from the job list until when your code will no
 * longer touch them.
 *
 * The code below relies on `job->status` having been set to FOREGROUND
 * and `job->num_processes_alive` having been set to the number of
 * processes successfully forked for this job.
 */
static void
wait_for_job(struct job *job)
{
    assert(signal_is_blocked(SIGCHLD));
    while (job->status == FOREGROUND && job->num_processes_alive > 0) {
        int status;

        pid_t child = waitpid(-1, &status, WUNTRACED);

        // When called here, any error returned by waitpid indicates a logic
        // bug in the shell.
        // In particular, ECHILD "No child process" means that there has
        // already been a successful waitpid() call that reaped the child, so
        // there's likely a bug in handle_child_status where it failed to update
        // the "job" status and/or num_processes_alive fields in the required
        // fashion.
        // Since SIGCHLD is blocked, there cannot be races where a child's exit
        // was handled via the SIGCHLD signal handler.
        if (child != -1)
            handle_child_status(child, status);
        else
            utils_fatal_error("waitpid failed, see code for explanation");
    }
}

/*
* NEEDS COMMENT, USER METHOD
*/
static struct job *
find_job_with_pid(pid_t pid)
{
    struct list_elem *e;

    for (e = list_begin(&job_list); e != list_end(&job_list); e = list_next(e)) {
        struct job *j = list_entry(e, struct job, elem);
        if (j->num_cmds > 1)
        {
            for (int i = 0; i < j->num_cmds; i++)
            {
                if (j->pids[i] == pid) {
                return j;
                }
            }
        }
        else {
            if (j->pid == pid) {
                return j;
                }
        }
    }

    return NULL;
}

/*
* NEEDS COMMENT, HANDLES CHILD STATUS + SIGNALS, still needs to handle a killsig
*/
static void
handle_child_status(pid_t pid, int status)
{
    assert(signal_is_blocked(SIGCHLD));
    struct job *job = find_job_with_pid(pid);

    if (WIFEXITED(status)){
        job->num_processes_alive--;
        if (job->num_processes_alive == 0 && job->status == FOREGROUND) {
            termstate_sample();
        }
    }
    else if(WIFSTOPPED(status))
    {
        int stopSig = WSTOPSIG(status);

        if (job != NULL) {
            job->status = STOPPED;
            termstate_save(&job->saved_tty_state);
            job->changed_tty = 1;
            if (stopSig == SIGTSTP) {
                print_job(job);
                kill(-job->pid, SIGTSTP);
            }
        }
    } 
    else if(WIFSIGNALED(status)) {
        int termSig = WTERMSIG(status);
        if (termSig != SIGINT) {
            printf("%s\n", strsignal(termSig));
        }
        job->num_processes_alive--;
    }
}

static void 
cleanup_completed_jobs(void) {
    struct list_elem *e = list_begin(&job_list);
    while (e != list_end(&job_list)) {
        struct job *job = list_entry(e, struct job, elem);
        struct list_elem *next = list_next(e);

        if (job->num_processes_alive == 0) {
            if (job->status == BACKGROUND)
            {
                printf("[%d]     Done\n", job->jid);
            }
            list_remove(e);
            delete_job(job);
        }

        e = next;
    }
}

int
main(int ac, char *av[])
{
    int opt;

    /* Process command-line arguments. See getopt(3) */
    while ((opt = getopt(ac, av, "h")) > 0) {
        switch (opt) {
        case 'h':
            usage(av[0]);
            break;
        }
    }

    list_init(&job_list);
    signal_set_handler(SIGCHLD, sigchld_handler);
    termstate_init();
    static struct termios tty;

    /* 
    Beginning of history library use and vars

        For reference:

        typedef struct _hist_state {
         HIST_ENTRY **entries; Pointer to the entries themselves. 
         int offset;           The location pointer within this array. 
         int length;           Number of elements within this array. 
         int size;             Number of slots allocated to this array. 
         int flags;
       } HISTORY_STATE;

     */

    /* Read/eval loop. */
    for (;;) {
        
        termstate_sample();
        cleanup_completed_jobs();

        /* If you fail this assertion, you were about to enter readline()
         * while SIGCHLD is blocked.  This means that your shell would be
         * unable to receive SIGCHLD signals, and thus would be unable to
         * wait for background jobs that may finish while the
         * shell is sitting at the prompt waiting for user input.
         */
        assert(!signal_is_blocked(SIGCHLD));

        /* If you fail this assertion, you were about to call readline()
         * without having terminal ownership.
         * This would lead to the suspension of your shell with SIGTTOU.
         * Make sure that you call termstate_give_terminal_back_to_shell()
         * before returning here on all paths.
         */
        assert(termstate_get_current_terminal_owner() == getpgrp());

        /* Do not output a prompt unless shell's stdin is a terminal */
        char * prompt = isatty(0) ? build_prompt() : NULL;
        char * cmdline = readline(prompt);
        free (prompt);


        /* Add the current command line to history */
        if (cmdline[0] != '!') {
            add_history(cmdline);
        }

        if (cmdline[0] == '!') {
            char * expansion;
            int result;
            result = history_expand(cmdline, &expansion);
            cmdline = expansion;

            if (result < 0) {
                printf("%s\n", cmdline);
                continue;
            }
            
            if (result == 2) {
                free(expansion);
                continue;
            }

            add_history(expansion);
        }

        if (cmdline == NULL)  /* User typed EOF */
            break;

        struct ast_command_line * cline = ast_parse_command_line(cmdline);

        free (cmdline);
        if (cline == NULL)                  /* Error in command line */
            continue;

        if (list_empty(&cline->pipes)) {    /* User hit enter */
            ast_command_line_free(cline);
            continue;
        }
        
        else {
            struct list_elem* e;
            for (e = list_begin(&cline->pipes); e != list_end(&cline->pipes); e = list_next(e))
            {
                //struct ast_pipeline *pipeline = list_entry(list_begin(&cline->pipes), struct ast_pipeline, elem);
                struct ast_pipeline *pipeline = list_entry(e, struct ast_pipeline, elem);
                struct ast_command *cmd = list_entry(list_begin(&pipeline->commands), struct ast_command, elem);

                // History expansion methodology
                // char* expansion;
                // int result;

                if (strcmp(cmd->argv[0], "exit") == 0) // Using "exit" to quit cush
                {
                    exit(EXIT_SUCCESS);
                }

                else if (strcmp(cmd->argv[0], "jobs") == 0)
                {
                    cleanup_completed_jobs();
                    struct list_elem* e;
                    for (e = list_begin(&job_list); e != list_end(&job_list); e = list_next(e))
                    {
                        struct job *job = list_entry(e, struct job, elem);
                        print_job(job);
                    }
                }

                /* Foreground processes require a wait, while background processes do not. That's what differentiates them. */
                else if (strcmp(cmd->argv[0], "fg") == 0) {

                    if (cmd->argv[1] == NULL) {
                        printf("fg: job id missing\n");
                        continue;
                    }

                    struct job *job = get_job_from_jid(atoi(cmd->argv[1]));

                    if (job == NULL) {
                        printf("%s %s: No such job\n", cmd->argv[0], cmd->argv[1]);
                        continue;
                    }
                    print_cmdline(job->pipe);
                    printf("\n");

                    job->status = FOREGROUND;

                    kill(-getpgid(job->pid), SIGCONT);

                    sigset_t mask, oldmask;
                    sigemptyset(&mask);
                    sigaddset(&mask, SIGCHLD);
                    sigprocmask(SIG_BLOCK, &mask, &oldmask);

                    termstate_save(&tty);
                    if (job->changed_tty == 1) {
                        termstate_give_terminal_to(&job->saved_tty_state, job->pid);
                    }
                    else {
                        termstate_give_terminal_to(&tty, job->pid);
                    }

                    wait_for_job(job);
                    termstate_give_terminal_back_to_shell();
                    sigprocmask(SIG_SETMASK, &oldmask, NULL);
                }


                else if (strcmp(cmd->argv[0], "bg") == 0) {

                    if (cmd->argv[1] == NULL) {
                        printf("bg: job id missing\n");
                        continue;
                    }

                    struct job *job = get_job_from_jid(atoi(cmd->argv[1]));
                    termstate_save(&job->saved_tty_state);
                    if (job == NULL) {
                        printf("%s %s: No such job\n", cmd->argv[0], cmd->argv[1]);
                        continue;
                    }
                    print_cmdline(job->pipe);
                    printf("\n");

                    kill(-getpgid(job->pid), SIGCONT);
                    tcsetpgrp(STDIN_FILENO, getpgid(job->pid));
                    job->status = BACKGROUND;
                    termstate_give_terminal_back_to_shell();
                }

                else if (strcmp(cmd->argv[0], "kill") == 0) { // Keep track of jobs, the job ID keeps changing

                    if (cmd->argv[1] == NULL) {
                        printf("kill: job id missing\n");
                        continue;
                    }

                    struct job *job = get_job_from_jid(atoi(cmd->argv[1]));
                    if (job == NULL) {
                        printf("%s %s: No such job\n", cmd->argv[0], cmd->argv[1]);
                        continue;
                    }
                    kill(-getpgid(job->pid), SIGTERM);
                }

                else if (strcmp(cmd->argv[0], "stop") == 0) { // Keep track of jobs here too

                    if (cmd->argv[1] == NULL) {
                        printf("stop: job id missing\n");
                        continue;
                    }

                    struct job *job = get_job_from_jid(atoi(cmd->argv[1]));
                    termstate_save(&job->saved_tty_state);
                    if (job == NULL) {
                        printf("%s %s: No such job\n", cmd->argv[0], cmd->argv[1]);
                        continue;
                    }
                    kill(-getpgid(job->pid), SIGSTOP);
                }

                else if (strcmp(cmd->argv[0], "cd") == 0) {

                    char* dir = cmd->argv[1];

                    if (!dir) {
                        chdir(getenv("HOME"));
                    } else {
                        if (chdir(dir)) {
                            printf("chdir: No such file or directory\n");
                        }
                    }
                }

                else if (strcmp(cmd->argv[0], "history") == 0) {
                    HISTORY_STATE * current_hist_state = history_get_history_state();

                    for(int i = 0; i < current_hist_state->length; i++) {
                        HIST_ENTRY * current_entry = history_get(i + history_base);
                        printf("\t%d %s\n", i + history_base, current_entry->line);
                    }
                }

                else if (list_size(&pipeline->commands) == 1) {

                    posix_spawn_file_actions_t action;
                    posix_spawnattr_t attr;
                    pid_t pid;

                    posix_spawnattr_init(&attr);
                    posix_spawn_file_actions_init(&action);
                    posix_spawnattr_setflags(&attr, POSIX_SPAWN_SETPGROUP | POSIX_SPAWN_USEVFORK);
                    posix_spawnattr_setpgroup(&attr, 0);
                    posix_spawnattr_tcsetpgrp_np(&attr, POSIX_SPAWN_SETPGROUP | POSIX_SPAWN_TCSETPGROUP);

                    // In-progress I/O Redir setups. Do we need to close any FDs? We're using stdin and stdout.
                    // Do we need to close files?
                    // So far it seems to be working perfectly
                    if (pipeline->iored_output) {

                        int flags = O_WRONLY | O_CREAT | (pipeline->append_to_output ? O_APPEND : O_TRUNC);

                        posix_spawn_file_actions_addopen(&action, STDOUT_FILENO, pipeline->iored_output, flags, 0644);
                        posix_spawn_file_actions_adddup2(&action, STDOUT_FILENO, STDERR_FILENO);
                        
                    }
                    else if (pipeline->iored_input) {
                        posix_spawn_file_actions_addopen(&action, STDIN_FILENO, pipeline->iored_input, O_RDONLY, 0644);
                        posix_spawn_file_actions_adddup2(&action, STDIN_FILENO, STDERR_FILENO); 
                    }
                    

                    if(posix_spawnp(&pid, cmd->argv[0], &action, &attr, cmd->argv, environ) != 0)
                    {
                        fprintf(stderr, "%s: No such file or directory\n", cmd->argv[0]);
                        posix_spawn_file_actions_destroy(&action);
                        posix_spawnattr_destroy(&attr);
                        continue;
                    }

                    struct job* job = add_job(pipeline);
                    job->num_processes_alive = 1;
                    job->num_cmds = 1;
                    job->changed_tty = 0;
                    if (pipeline->bg_job) {
                        job->status = BACKGROUND;
                    }
                    else {
                        job->status = FOREGROUND;
                    }
                    job->pid = pid;

                    if (job->status == FOREGROUND) {
                        sigset_t mask, oldmask;
                        sigemptyset(&mask);
                        sigaddset(&mask, SIGCHLD);
                        sigprocmask(SIG_BLOCK, &mask, &oldmask);
                        tcsetpgrp(STDIN_FILENO, pid);
                        wait_for_job(job);
                        sigprocmask(SIG_SETMASK, &oldmask, NULL);
                        termstate_give_terminal_back_to_shell();
                    }
                    else{
                        printf("[%d] %d\n", job->jid, job->pid);
                    }
                    posix_spawn_file_actions_destroy(&action);
                    posix_spawnattr_destroy(&attr);
                }
                else {
                    ast_command_line_print(cline);
                    int num_cmds = list_size(&pipeline->commands);
                    int pipe_fds[2*num_cmds-2];
                    pid_t pids[num_cmds];

                    for (int i = 0; i < num_cmds - 1; i++) {
                        if (pipe(pipe_fds + i*2) < 0) {
                            perror("pipe");
                            exit(EXIT_FAILURE);
                        }
                    }

                    struct list_elem* e = list_begin(&pipeline->commands);
                    int cmd_index = 0;

                    sigset_t mask, oldmask;
                    sigemptyset(&mask);
                    sigaddset(&mask, SIGCHLD);
                    sigprocmask(SIG_BLOCK, &mask, &oldmask);

                    while (e != list_end(&pipeline->commands)) {
                        struct ast_command *cmd = list_entry(e, struct ast_command, elem);

                        printf("%s\n", cmd->argv[0]);

                        posix_spawn_file_actions_t action;
                        posix_spawn_file_actions_init(&action);
                        posix_spawnattr_t attr;
                        posix_spawnattr_init(&attr);
                        posix_spawnattr_setflags(&attr, POSIX_SPAWN_SETPGROUP | POSIX_SPAWN_USEVFORK);

                        if (cmd_index == 0)
                        {
                            posix_spawnattr_setpgroup(&attr, 0);
                            posix_spawnattr_tcsetpgrp_np(&attr, POSIX_SPAWN_SETPGROUP | POSIX_SPAWN_TCSETPGROUP);
                        }
                        else
                        {
                            posix_spawnattr_setpgroup(&attr, pids[0]);
                        }

                        if (pipeline->iored_output && cmd_index == num_cmds - 1) {
                            int flags = O_WRONLY | O_CREAT | (pipeline->append_to_output ? O_APPEND : O_TRUNC);
                            posix_spawn_file_actions_addopen(&action, STDOUT_FILENO, pipeline->iored_output, flags, 0644);
                        }
                        else if (pipeline->iored_input && cmd_index == 0) {
                            posix_spawn_file_actions_addopen(&action, STDIN_FILENO, pipeline->iored_input, O_RDONLY, 0644);
                            posix_spawn_file_actions_adddup2(&action, STDIN_FILENO, STDERR_FILENO); 
                        }

                        if (cmd_index > 0) {
                            posix_spawn_file_actions_adddup2(&action, pipe_fds[(cmd_index-1)*2], STDIN_FILENO);
                            posix_spawn_file_actions_addclose(&action, pipe_fds[(cmd_index-1)*2+1]);

                            if (cmd->dup_stderr_to_stdout) {
                                posix_spawn_file_actions_adddup2(&action, STDOUT_FILENO, STDERR_FILENO);
                            }
                        }

                        if (cmd_index < num_cmds - 1) {
                            posix_spawn_file_actions_adddup2(&action, pipe_fds[cmd_index*2+1], STDOUT_FILENO);
                            posix_spawn_file_actions_addclose(&action, pipe_fds[cmd_index*2]);

                            if (cmd->dup_stderr_to_stdout) {
                                posix_spawn_file_actions_adddup2(&action, STDOUT_FILENO, STDERR_FILENO);
                            } 
                        }

                        for (int i = 0; i < 2*num_cmds-2; i++) {
                                posix_spawn_file_actions_addclose(&action, pipe_fds[i]);
                        }

                        for (int j = 0; j < 2*(num_cmds-1); j++) {
                            if (j != (cmd_index-1)*2 && j != cmd_index*2+1) {
                                posix_spawn_file_actions_addclose(&action, pipe_fds[j]);
                            }
                        }

                        pid_t pid;
                        if (posix_spawnp(&pid, cmd->argv[0], &action, &attr, cmd->argv, environ) != 0) {
                            perror("posix_spawnp");
                            posix_spawn_file_actions_destroy(&action);
                            posix_spawnattr_destroy(&attr);
                            exit(EXIT_FAILURE);
                        }

                        pids[cmd_index] = pid;

                        posix_spawn_file_actions_destroy(&action);
                        posix_spawnattr_destroy(&attr);

                        e = list_next(e);
                        cmd_index++;
                    }

                    for (int i = 0; i < 2*num_cmds-2; i++) {
                        close(pipe_fds[i]);
                    }
                    struct job* job = add_job(pipeline);
                    job->num_processes_alive = num_cmds;
                    job->pids = malloc(num_cmds * sizeof(pid_t));
                    job->num_cmds = num_cmds;
                    job->changed_tty = 0;

                    for (int i = 0; i < num_cmds; i++) {
                        job->pids[i] = pids[i];
                    }
                    job->pid = job->pids[0];
                    sigprocmask(SIG_SETMASK, &oldmask, NULL);
                    if (pipeline->bg_job)
                    {
                        job->status = BACKGROUND;
                        printf("[%d] %d\n", job->jid, job->pid);
                    }
                    else {
                        sigset_t mask, oldmask;
                        sigemptyset(&mask);
                        sigaddset(&mask, SIGCHLD);
                        sigprocmask(SIG_BLOCK, &mask, &oldmask);
                        tcsetpgrp(STDIN_FILENO, pids[0]);
                        wait_for_job(job);
                        sigprocmask(SIG_SETMASK, &oldmask, NULL);
                        termstate_give_terminal_back_to_shell();
                    }
                }

            }
        }

        //ast_command_line_print(cline);      /* Output a representation of
                                               //the entered command line */

        /* Free the command line.
         * This will free the ast_pipeline objects still contained
         * in the ast_command_line.  Once you implement a job list
         * that may take ownership of ast_pipeline objects that are
         * associated with jobs you will need to reconsider how you
         * manage the lifetime of the associated ast_pipelines.
         * Otherwise, freeing here will cause use-after-free errors.
         */
        //ast_command_line_free(cline);

        
    }
    return 0;
}
