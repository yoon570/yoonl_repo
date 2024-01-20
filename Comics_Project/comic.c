//"I have neither given nor received unauthorized assistance on this assignment." – Yoon Lee, yoonl18

#include "comic.h"

//Helper methods start here

//Initializes comic and assigns memory
void init_comic(struct Comic * c, char* date, char* code, char* publisher, char* title, char* cost)
{
    c->date = calloc(strlen(date) + 1, sizeof(char));
    c->code = calloc(strlen(code) + 1, sizeof(char));
    c->publisher = calloc(strlen(publisher) + 1, sizeof(char));
    c->title = calloc(strlen(title) + 1, sizeof(char));
    c->cost = calloc(strlen(cost) + 1, sizeof(char));
}

//Displays comic into output file with appropriate format
void display_comic(struct Comic c, FILE* out)
{
    fprintf(out, "\tDate: %s\n", c.date);
    fprintf(out, "\tCode: %s\n", c.code);
    fprintf(out, "\tPublisher: %s\n", c.publisher);
    fprintf(out, "\tTitle: %s\n", c.title);
    fprintf(out, "\tCost: %s\n", c.cost);
}

//Updates each field in a comic struct
void update_comic(struct Comic * c, char* date, char* code, char* publisher, char* title, char* cost)
{
    strcpy(c->date, date);
    strcpy(c->code, code);
    strcpy(c->publisher, publisher);
    strcpy(c->title, title);
    strcpy(c->cost, cost);
}

//Prints comic to a file horizontally with fields separated by commas
void display_comic_horizontal(struct Comic c, FILE* out)
{
    fprintf(out, "%s,%s,%s,%s,%s\n", c.date, c.code, c.publisher, c.title, c.cost);
}

//Reads all fields for a comic from a given file
void read_fields(FILE* library, char* date, char* code, char* publisher, char* title, char* cost)
{
    fscanf(library, "%[^,],%[^,],%[^,],%[^,],%s\n", date, code, publisher, title, cost);
}

//Initializes a list and allocates the necessary memory
void init_list(struct Comic_List * list)
{
    list->list = calloc(10, sizeof(struct Comic));
    list->size = 10;
    list->count = 0;
}

//Doubles the capacity of a list
void doubling(struct Comic_List * list)
{
    list->list = realloc(list->list, list->size * 2 * sizeof(struct Comic));
    list->size = 2 * list->size;
}

//Adds a comic to a given list
void add_comic(struct Comic_List * list, struct Comic c)
{
    if (list->count == list->size)
    {
        doubling(list);
    }

    list->list[list->count] = c;
    list->count = list->count + 1;
}

void free_comic(struct Comic * c)
{
    free(c->date);
    free(c->code);
    free(c->publisher);
    free(c->title);
    free(c->cost);
}

void free_list(struct Comic_List * list)
{
    for (int i = 0; i < list->count; i++)
    {
        free_comic(&list->list[i]);
    }
    free(list->list);
}

//Helper methods end here

//Required methods start here

//Loads a comic from a given file
void load(struct Comic_List * list, char * file_name, FILE* out)
{
    //Sets up local variables for each comic field
    char date[500] = {'\0'};
    char code[500] = {'\0'};
    char publisher[500] = {'\0'};
    char title[500] = {'\0'};
    char cost[500] = {'\0'};
    //Open library csv file using string param
    FILE* library = fopen(file_name, "r");

    //skip first line
    char str[100];
    fgets(str, 100, library);

    int counter = 0;

    //Loop through library
    while(!feof(library))
    {
        //Read all fields from given line
        read_fields(library, date, code, publisher, title, cost);
        
        //Create comic struct and assign memory
        struct Comic item;
        init_comic(&item, date, code, publisher, title, cost);

        //Assign parameters to each respective part of a comic
        update_comic(&item, date, code, publisher, title, cost);

        //Doubles list size if it is too small
        if (list->count == list->size)
        {
            doubling(list);
        }

        //Call for add_comic method to list
        add_comic(list, item);
        counter++;
    }
    //Print the number of comics loaded to the output file
    fprintf(out, "\tNumber of comics: %d\n", counter);
    fclose(library);
}

//Prints comics to output file
void display(struct Comic_List list, FILE* out)
{
    if (list.count == 0)
    {
        fprintf(out, "List is currently empty.\n");
    }

    for (int i = 0; i < list.count; i++)
    {
        fprintf(out, "Comic Number: %d\n", i + 1);
        display_comic(list.list[i], out);
    }
}

//Saves a given comic list to a file
void save(struct Comic_List list, char * save_file)
{
    FILE * write_file = fopen(save_file, "w");
    //Prints header for the CSV file
    fprintf(write_file, "DATE,CODE,PUBLISHER,TITLE,COST\n");

    //Iterates through list and displays comic in CSV format
    for (int i = 0; i < list.count; i++)
    {
        display_comic_horizontal(list.list[i], write_file);
    }

    fclose(write_file);
}

//Clears the list
void clear(struct Comic_List * list)
{
    list->count = 0;
}

//Finds a comic at a given index
void find(struct Comic_List list, int index, FILE * out)
{
    //Return false if index is greater than count
    if (index > list.count)
    {
        //move print to main when writing final code
        fprintf(out, "There is no comic at index #%d in the list.\n", index);
    }
    else 
    {
    //Print to file using display_comic
    display_comic(list.list[index], out);
    }
}

void remove_list(struct Comic_List * list, int index, FILE * out)
{
    if (index > list->count)
    {
        fprintf(out, "Comic at index %d was not removed\n", index);
        //Comic at index 52 was not removed
    }
    else
    {
        //Frees the memory associated with a given comic

        free_comic(&list->list[index]);

        for (int i = index; i < list->count; i++)
        {
            list->list[i] = list->list[i + 1];
        }

        

        list->count = list->count - 1;

        fprintf(out, "Comic at index %d successfully removed\n", index);
    }
}

//Adds a comic from a given list @ an index to a cart list
void buy(struct Comic_List list, struct Comic_List * cart, int index, FILE* out)
{
    if (index > list.count)
    {
        //move print to main when writing final code
        fprintf(out, "There is no comic at index #%d in the list.\n", index);
    }
    else
    {
    //Call for add_comic method to cart
    add_comic(cart, list.list[index]);
    fprintf(out, "Comic #%d added to purchase list\n", index);
    }
}

//Prints out cart and necessary transactional totals
void checkout(struct Comic_List cart, FILE* out)
{
    double total = 0.0;
    fprintf(out, "Comics in Purchase List\n");
    //Prints all comics
    display(cart, out);
    //Prints subtotal, tax, and total

    for (int i = 0; i < cart.count; i++)
    {
        if (strcmp(cart.list[i].cost, "AR") != 0)
        {

            total = total + atof(cart.list[i].cost + 1);
        }
    }

    fprintf(out, " Subtotal: $%.2lf\n", total);
    fprintf(out, "      Tax: $%.2lf\n", total * 0.05);
    fprintf(out, "    Total: $%.2lf\n", total + total * 0.05);

    free_list(&cart);
}