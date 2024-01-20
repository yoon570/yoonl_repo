//Command directory
        // void display_comic(struct Comic c, FILE* out);
        // void display_comic_horizontal(struct Comic.c FILE* out);
        // void update_comic(struct Comic * c, char* date, char* code, char* publisher, char* title, char* cost);
        // void copy_comic(struct Comic * src, struct Comic * dest);
        // void free_comic(struct Comic * c);
        // void read_fields(FILE* library, char* date, char* code, char* publisher, char* title, char* cost);
        // void init_list(struct Comic_List * list);
        // void doubling(struct Comic_List * list);
        // void display_list_horizontal(struct Comic_List list, FILE* out);
        // void add_comic(struct Comic_List * list, struct Comic c);

        // void display(struct Comic_List list, FILE* out);
        // void load(struct Comic_List list, char * library);

    //load X O
    //buy X
    //checkout X
    //display X O
    //save X O
    //clear X O
    //find X O
    //remove X O

//Helper methods start here

//Initializes comic and assigns memory
void init_comic(struct Comic * item)
{
    item.date = calloc(10, (strlen(date) + 1) * sizeof(char));
    item.code = calloc(10, (strlen(code) + 1) * sizeof(char));
    item.publisher = calloc(10, (strlen(publisher) + 1) * sizeof(char));
    item.title = calloc(10, (strlen(title) + 1) * sizeof(char));
    item.cost = calloc(10, (strlen(cost) + 1) * sizeof(char));
}

//Displays comic into output file with appropriate format
void display_comic(struct Comic c, FILE* out)
{
    fprintf(out, "\t%s\n", c.date);
    fprintf(out, "\t%s\n", c.code);
    fprintf(out, "\t%s\n", c.publisher);
    fprintf(out, "\t%s\n", c.title);
    fprintf(out, "\t%s\n", c.cost);
}

void free_list(struct Comic_List * list)
{
    for (int i = 0; i < list.count; )
}

//Prints comic to a file horizontally with fields separated by commas
void display_comic_horizontal(struct Comic c, FILE* out)
{
    fprintf(out, "%s,%s,%s,%s,%s\n", c.date, c.code, c.publisher, c.title, c.cost);
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

//Copies comic fields from a source to a destination
void copy_comic(struct Comic * src, struct Comic * dest)
{
    strcpy(src->date, dest->date);
    strcpy(src->code, dest->code);
    strcpy(src->publisher, dest->publisher);
    strcpy(src->title, dest->title);
    strcpy(src->cost, dest->cost);
}

//Frees the memory associated with a comic
void free_comic(struct Comic * c)
{
    free(c->date);
    free(c->code);
    free(c->publisher);
    free(c->title);
    free(c->cost);
}

//Reads all fields for a comic from a given file
void read_fields(FILE* library, char* date, char* code, char* publisher, char* title, char* cost)
{
    fscanf(library, "%[^,],%[^,],%[^,],%[^,],%s\n", date, code, publisher, title, cost);
}

//Initializes a list and allocates the necessary memory
void init_list(struct Comic_List * list)
{
    list->list = malloc(10 * sizeof(struct Comic));
    list->size = 10;
    list->count = 0;
}

//Doubles the capacity of a list
void doubling(struct Comic_List * list)
{
    list->list = realloc(list->list, list->size * 2 * sizeof(struct Comic));
    list->size = 2 * list->size;
}

//Displays all elements in a list in CSV format
void display_list_horizontal(struct Comic_List list, FILE* out)
{
    for (int i = 0; i < list.count; i++)
    {
        display_comic_horizontal(list.list[i]);
    }
}

//Adds a comic to a given list
void add_comic(struct Comic_List * list, struct Comic c)
{
    if (list->count == list->size)
    {
        doubling(list);
    }

    list->list[i] = c;
    list->count = list->count + 1;
}
//Helper methods end here


//Required methods start here
void display(struct Comic_List list, FILE* out)
{
    if (list.count == 0)
    {
        fprintf(out, "List is currently empty.\n");
    }

    for (int i = 0; i < list.count; i++)
    {
        fprintf(out, "Comic Number: %d\n", i);
        display_comic(list.list[i], out);
    }
}

//Loads a comic from a given file
void load(struct Comic_List list, char * library)
{
    //Sets up local variables for each comic field
    char date[500] = {'\0'};
    char code[500] = {'\0'};
    char publisher[500] = {'\0'};
    char title[500] = {'\0'};
    char cost[500] = {'\0'};
    //Open library csv file using string param
    FILE* library = fopen(load_file, "r");

    //skip first line
    char str[100];
    fgets(str, 100, library);

    //Loop through library
    while(!feof(library))
    {
        //Read all fields from given line
        read_fields(library, date, code, publisher, title, cost);
        
        //Create comic struct and assign memory
        struct Comic item;
        init_comic(item);

        //Assign parameters to each respective part of a comic
        update_comic(item, date, code, publisher, title, cost);
        fprintf(stderr, "%s\n", item);
        fprintf(stderr, "%s\n", date);
        fprintf(stderr, "%s\n", code);
        fprintf(stderr, "%s\n", publisher);
        fprintf(stderr, "%s\n", cost);

        //Doubles list size if it is too small
        if (list->count == list->size)
        {
            doubling(list);
        }

        //Call for add_comic method to list
        add_comic(list, item);

    }
    //Print the number of comics loaded to the output file
    fprintf(out, "\tNumber of comics: %d\n", list->count);
    fclose(out);
    fclose(library);
}

//Saves a given comic list to a file
void save(struct Comic_List * list, FILE * save_file)
{
    //Prints header for the CSV file
    fprintf(out, "DATE,CODE,PUBLISHER,TITLE,COST\n");

    //Iterates through list and displays comic in CSV format
    for (int i = 0; i < list->count; i++)
    {
        display_comic_horizontal(list->list[i], save_file);
    }
}

//Finds a comic at a given index
int find(struct Comic_List * list, int index, FILE * out)
{
    //Return false if index is greater than count
    if (index > list->count || index == 0)
    {
        //move print to main when writing final code
        fprintf(out, "There is no comic at index #%d in the list.\n", index);
        return 0;
    }

    //Print to file using display_comic
    display_comic(list->list[index - 1], out);
}

//Adds a comic from a given list @ an index to a cart list
int buy(struct Comic_List * list, struct Comic_List * cart, int index)
{
    if (index > list->count || index == 0)
    {
        //move print to main when writing final code
        fprintf(out, "There is no comic at index #%d in the list.\n", index);
        return 0;
    }

    struct Comic purchase;
    init_comic(purchase);

    copy_comic(list->list[index-1], purchase);

    //Call for add_comic method to cart
    add_comic(cart, purchase);
    cart->count = cart->count + 1;
}

//Prints out cart and necessary transactional totals
void checkout(struct Comic_List cart, FILE* out)
{
    //Collects and sums up all costs of all comics in the cart
    double total = 0;
    for (int i = 0; i < cart->count; i++)
    {
        total = total + atof(cart.list[i].cost);
    }

    fprintf(out, "Comics in Purchase List\n");
    //Prints all comics
    display(cart, out);
    //Prints subtotal, tax, and total
    fprintf(out, " Subtotal: %f\n", total);
    fprintf(out, "      Tax: %f\n", total * 0.05);
    fprintf(out, "    Total: %f\n", total + total * 0.05);
}

//Clears the list
void clear(struct Comic_list * list)
{
    list->count = 0;
}

void remove(struct Comic_List * list, int index)
{
    if (index > list->count)
    {
        return 0;
        //Comic at index 52 was not removed
    }

    //Frees the memory associated with a given comic
    free_comic(list->list[index - 1]);

    for (int i = index ; i < list->count; i++)
    {
        list->list[i - 1] = list->list[i];
    }

    list->count = list->count - 1;

    return 1;
    //Comic at index 12 successfully removed
}