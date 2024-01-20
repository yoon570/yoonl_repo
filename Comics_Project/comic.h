//"I have neither given nor received unauthorized assistance on this assignment." – Yoon Lee, yoonl18

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

//Struct declaration for comic list
struct Comic_List
{
    struct Comic* list;
    int size;
    int count;
};

//Struct declaration for comic
struct Comic
{
    char* date;
    char* code;
    char* publisher;
    char* title;
    char* cost;
};

void init_comic(struct Comic * c, char* date, char* code, char* publisher, char* title, char* cost);
void display_comic(struct Comic c, FILE* out);
void update_comic(struct Comic * c, char* date, char* code, char* publisher, char* title, char* cost);
void read_fields(FILE* library, char* date, char* code, char* publisher, char* title, char* cost);
void init_list(struct Comic_List * list);
void doubling(struct Comic_List * list);
void add_comic(struct Comic_List * list, struct Comic c);
void display(struct Comic_List list, FILE* out);
void load(struct Comic_List * list, char * file_name, FILE* out);
void save(struct Comic_List list, char * save_file);
void display_comic_horizontal(struct Comic c, FILE* out);
void clear(struct Comic_List * list);
void find(struct Comic_List list, int index, FILE * out);
void remove_list(struct Comic_List * list, int index, FILE * out);
void buy(struct Comic_List list, struct Comic_List * cart, int index, FILE* out);
void checkout(struct Comic_List cart, FILE* out);
void free_comic(struct Comic * c);
void free_list(struct Comic_List * list);