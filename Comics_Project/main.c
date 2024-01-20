#include "comic.h"
#include <string.h>

int main( int argc, char** argv )
{
	if ( argc != 3 )
	{
		fprintf(stderr, "USAGE: %s <input file> <output file>\n", argv[0] );
		exit(1);
	}

	FILE* in = fopen(argv[1], "r");
	FILE* out = fopen(argv[2], "w");

    struct Comic_List save_list;
    struct Comic_List buy_list;

	init_list(&save_list);
    init_list(&buy_list);
	
	char command[256] = {'\0'};
    char param[256] = {'\0'};
    int index = 0;

    //Scan first command and begin reading command input file
    fscanf(in, "%s", command);
	while(!feof(in))
	{
        //Searches for each respective command possibility
		if (strcmp(command, "load" ) == 0)
        {
            fprintf(out, "Command: %s", command);
            fscanf(in, "%s\n", param);
            fprintf(out, " %s\n", param);
            load(&save_list, param, out);
        }

        if (strcmp(command, "display") == 0)
        {
            fprintf(out, "Command: %s\n", command);
            display(save_list, out);
        }

        if (strcmp(command, "clear") == 0)
        {
            fprintf(out, "Command: %s\n", command);
            clear(&save_list);
        }

        if (strcmp(command, "save" ) == 0)
        {
            fprintf(out, "Command: %s", command);
            fscanf(in, "%s\n", param);
            fprintf(out, " %s\n", param);
            save(save_list, param);
        }

        if (strcmp(command, "find" ) == 0)
        {
            fprintf(out, "Command: %s", command);
            fscanf(in, "%d", &index);
            fprintf(out, " %d\n", index);
            find(save_list, index, out);
        }

        if (strcmp(command, "remove" ) == 0)
        {
            fprintf(out, "Command: %s", command);
            fscanf(in, "%d", &index);
            fprintf(out, " %d\n", index);
            remove_list(&save_list, index, out);
        }
        
        if (strcmp(command, "buy" ) == 0)
        {
            fprintf(out, "Command: %s", command);
            fscanf(in, "%d", &index);
            fprintf(out, " %d\n", index);
            buy(save_list, &buy_list, index, out);
        }

        if (strcmp(command, "checkout") == 0)
        {
            fprintf(out, "Command: %s\n", command);
            checkout(buy_list, out);
        }

        //Scan new command
        fscanf(in, "%s", command);
    }
    
    free_list(&save_list);
    free_list(&buy_list);
    
    fclose(in);
    fclose(out);
    
    return 0;
}