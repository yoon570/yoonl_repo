/*
 * A partial implementation of HTTP/1.0
 *
 * This code is mainly intended as a replacement for the book's 'tiny.c' server
 * It provides a *partial* implementation of HTTP/1.0 which can form a basis for
 * the assignment.
 *
 * @author G. Back for CS 3214 Spring 2018
 */
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdbool.h>
#include <errno.h>
#include <unistd.h>
#include <time.h>
#include <fcntl.h>
#include <assert.h>
#include <linux/limits.h>
#include <dirent.h>
#include <jansson.h>

#include "http.h"
#include "hexdump.h"
#include "socket.h"
#include "bufio.h"
#include "main.h"

// Need macros here because of the sizeof
#define CRLF "\r\n"
#define CR "\r"
#define STARTS_WITH(field_name, header) \
    (!strncasecmp(field_name, header, sizeof(header) - 1))

/* Parse HTTP request line, setting req_method, req_path, and req_version. */
static bool
http_parse_request(struct http_transaction *ta)
{
    size_t req_offset;
    ssize_t len = bufio_readline(ta->client->bufio, &req_offset);
    if (len < 2)       // error, EOF, or less than 2 characters
        return false;

    char *request = bufio_offset2ptr(ta->client->bufio, req_offset);
    request[len-2] = '\0';  // replace LF with 0 to ensure zero-termination
    char *endptr;
    char *method = strtok_r(request, " ", &endptr);
    if (method == NULL)
        return false;

    if (!strcmp(method, "GET"))
        ta->req_method = HTTP_GET;
    else if (!strcmp(method, "POST"))
        ta->req_method = HTTP_POST;
    else
        ta->req_method = HTTP_UNKNOWN;

    char *req_path = strtok_r(NULL, " ", &endptr);
    if (req_path == NULL)
        return false;

    ta->req_path = bufio_ptr2offset(ta->client->bufio, req_path);

    char *http_version = strtok_r(NULL, CR, &endptr);
    if (http_version == NULL)  // would be HTTP 0.9
        return false;

    // record client's HTTP version in request
    if (!strcmp(http_version, "HTTP/1.1"))
        ta->req_version = HTTP_1_1;
    else if (!strcmp(http_version, "HTTP/1.0"))
        ta->req_version = HTTP_1_0;
    else
        return false;

    return true;
}

/* Error catch method for cookie creation */
static void 
die(const char *msg, int error)
{
    fprintf(stderr, "%s: %s\n", msg, strerror(error));
    exit(EXIT_FAILURE);
}

/*
static bool
check_token(char* encoded_token) {
    jwt_t *ymtoken;
    
    char *key = getenv("SECRET");
    if (key == NULL) {
        exit (EXIT_FAILURE);
    }

    int rc = jwt_decode(&ymtoken, encoded_token, (unsigned char *)key, strlen(key));
    if (rc)
        die("jwt_decode", rc);

    free(encoded_token);

    char *grants = jwt_get_grants_json(ymtoken, NULL); // NULL means all
    if (grants == NULL)
        die("jwt_get_grants_json", ENOMEM);

    jwt_free(ymtoken);
    
    json_error_t error;
    json_t *jgrants = json_loadb(grants, strlen(grants), 0, &error);
    if (jgrants == NULL)
        die("json_loadb", EINVAL);

    free (grants);

    json_int_t exp, iat;
    const char *sub;

    rc = json_unpack(jgrants, "{s:I, s:I, s:s}", 
            "exp", &exp, "iat", &iat, "sub", &sub);
    if (rc == -1)
        die("json_unpack", EINVAL);

    if (strcmp(sub, getenv("USER_NAME")) && exp > 0) {
        return true;
    } else {
        return false;
    }
}
*/
/* Assigns boolean value for client authentication */
/*
static bool decode_token(char *cookie_header) {
    char *token_start = strstr(cookie_header, "auth_jwt_token=");
    if (!token_start) {
        return false;
    }

    token_start += strlen("auth_jwt_token=");
    char *token_end = strchr(token_start, ';');
    if (!token_end) {
        token_end = token_start + strlen(token_start);
    }

    char *token = strndup(token_start, token_end - token_start);
    bool valid = check_token(token);
    return valid;
}
*/
static char* get_jwt_from_cookie(const char* cookie_header) {
    const char* token_prefix = "auth_jwt_token=";
    char* result = NULL;

    char* cookies = strdup(cookie_header);  // Duplicate the cookie header for manipulation
    if (!cookies) {
        return NULL;
    }

    char* cookie = strtok(cookies, "; ");  // Split cookies using "; " as the delimiter
    while (cookie) {
        if (strncmp(cookie, token_prefix, strlen(token_prefix)) == 0) {  // Check if the token name matches
            char* token_start = cookie + strlen(token_prefix);
            char* token_end = strchr(token_start, ';');
            if (!token_end) {
                token_end = token_start + strlen(token_start);  // Handle case where the token is the last/only cookie
            }
            result = strndup(token_start, token_end - token_start);
            break;
        }
        cookie = strtok(NULL, "; ");
    }

    free(cookies);
    return result;
}

/* Process HTTP headers. */
static bool
http_process_headers(struct http_transaction *ta)
{
    for (;;) {
        size_t header_offset;
        ssize_t len = bufio_readline(ta->client->bufio, &header_offset);
        if (len <= 0)
            return false;

        char *header = bufio_offset2ptr(ta->client->bufio, header_offset);
        if (len == 2 && STARTS_WITH(header, CRLF))       // empty CRLF
            return true;

        header[len-2] = '\0';
        /* Each header field consists of a name followed by a 
         * colon (":") and the field value. Field names are 
         * case-insensitive. The field value MAY be preceded by 
         * any amount of LWS, though a single SP is preferred.
         */
        char *endptr;
        char *field_name = strtok_r(header, ":", &endptr);
        if (field_name == NULL)
            return false;

        // skip white space
        char *field_value = endptr;
        while (*field_value == ' ' || *field_value == '\t')
            field_value++;
        // you may print the header like so
        //printf("Header: %s: %s\n", field_name, field_value);
        if (!strcasecmp(field_name, "Content-Length")) {
            ta->req_content_len = atoi(field_value);
        }

        if (!strcasecmp(field_name, "Cookie")) {
            char *jwt_token = get_jwt_from_cookie(field_value);
            if (jwt_token) {
                printf("%s\n", jwt_token);
                ta->client->authenticated = true;
                free(jwt_token);
                ta->jwt_token = get_jwt_from_cookie(field_value);
            }
        }

        if (!strcasecmp(field_name, "Range")) {
            // Assume range format is "bytes=start-end"
            char *range_spec = strchr(field_value, '=') + 1;
            char *dash = strchr(range_spec, '-');
            if (dash) {
                *dash = '\0'; // Terminate start string
                char *start_str = range_spec;
                char *end_str = dash + 1;
                ta->range_start = atoi(start_str);
                ta->range_end = *end_str ? atoi(end_str) : -1; // -1 to denote open-ended range
                ta->has_range = true;
            }
        }

        /* Handle other headers here. Both field_value and field_name
         * are zero-terminated strings.
         */
    }
}

const int MAX_HEADER_LEN = 2048;

/* add a formatted header to the response buffer. */
void 
http_add_header(buffer_t * resp, char* key, char* fmt, ...)
{
    va_list ap;

    buffer_appends(resp, key);
    buffer_appends(resp, ": ");

    va_start(ap, fmt);
    char *error = buffer_ensure_capacity(resp, MAX_HEADER_LEN);
    int len = vsnprintf(error, MAX_HEADER_LEN, fmt, ap);
    resp->len += len > MAX_HEADER_LEN ? MAX_HEADER_LEN - 1 : len;
    va_end(ap);

    buffer_appends(resp, "\r\n");
}

/* add a content-length header. */
static void
add_content_length(buffer_t *res, size_t len)
{
    http_add_header(res, "Content-Length", "%ld", len);
}

/* start the response by writing the first line of the response 
 * to the response buffer.  Used in send_response_header */
static void
start_response(struct http_transaction * ta, buffer_t *res)
{
    buffer_init(res, 80);

    /* Hint: you must change this as you implement HTTP/1.1.
     * Respond with the highest version the client supports
     * as indicated in the version field of the request.
     */
    if(ta->req_version == HTTP_1_1)
    {
        buffer_appends(res, "HTTP/1.1 ");
    }
    else 
    {
        buffer_appends(res, "HTTP/1.0 ");
    }

    switch (ta->resp_status) {
    case HTTP_OK:
        buffer_appends(res, "200 OK");
        break;
    case HTTP_PARTIAL_CONTENT:
        buffer_appends(res, "206 Partial Content");
        break;
    case HTTP_BAD_REQUEST:
        buffer_appends(res, "400 Bad Request");
        break;
    case HTTP_PERMISSION_DENIED:
        buffer_appends(res, "403 Permission Denied");
        break;
    case HTTP_NOT_FOUND:
        buffer_appends(res, "404 Not Found");
        break;
    case HTTP_METHOD_NOT_ALLOWED:
        buffer_appends(res, "405 Method Not Allowed");
        break;
    case HTTP_REQUEST_TIMEOUT:
        buffer_appends(res, "408 Request Timeout");
        break;
    case HTTP_REQUEST_TOO_LONG:
        buffer_appends(res, "414 Request Too Long");
        break;
    case HTTP_NOT_IMPLEMENTED:
        buffer_appends(res, "501 Not Implemented");
        break;
    case HTTP_SERVICE_UNAVAILABLE:
        buffer_appends(res, "503 Service Unavailable");
        break;
    case HTTP_INTERNAL_ERROR:
        buffer_appends(res, "500 Internal Server Error");
        break;
    default:  /* else */
        buffer_appends(res, "500 This is not a valid status code."
                "Did you forget to set resp_status?");
        break;
    }
    buffer_appends(res, CRLF);
}

/* Send response headers to client in a single system call. */
static bool
send_response_header(struct http_transaction *ta)
{
    buffer_t response;
    start_response(ta, &response);
    buffer_appends(&ta->resp_headers, CRLF);

    buffer_t *response_and_headers[2] = {
        &response, &ta->resp_headers
    };

    int rc = bufio_sendbuffers(ta->client->bufio, response_and_headers, 2);
    buffer_delete(&response);
    return rc != -1;
}

/* Send a full response to client with the content in resp_body. */
static bool
send_response(struct http_transaction *ta)
{
    // add content-length.  All other headers must have already been set.
    add_content_length(&ta->resp_headers, ta->resp_body.len);
    buffer_appends(&ta->resp_headers, CRLF);

    buffer_t response;
    start_response(ta, &response);

    buffer_t *response_and_headers[3] = {
        &response, &ta->resp_headers, &ta->resp_body
    };

    int rc = bufio_sendbuffers(ta->client->bufio, response_and_headers, 3);
    buffer_delete(&response);
    return rc != -1;
}

const int MAX_ERROR_LEN = 2048;

/* Send an error response. */
static bool
send_error(struct http_transaction * ta, enum http_response_status status, const char *fmt, ...)
{
    va_list ap;

    va_start(ap, fmt);
    char *error = buffer_ensure_capacity(&ta->resp_body, MAX_ERROR_LEN);
    int len = vsnprintf(error, MAX_ERROR_LEN, fmt, ap);
    ta->resp_body.len += len > MAX_ERROR_LEN ? MAX_ERROR_LEN - 1 : len;
    va_end(ap);
    ta->resp_status = status;
    http_add_header(&ta->resp_headers, "Content-Type", "text/plain");
    return send_response(ta);
}

/* Send Not Found response. */
static bool
send_not_found(struct http_transaction *ta)
{
    return send_error(ta, HTTP_NOT_FOUND, "File %s not found", 
        bufio_offset2ptr(ta->client->bufio, ta->req_path));
}

/* A start at assigning an appropriate mime type.  Real-world 
 * servers use more extensive lists such as /etc/mime.types
 */
static const char *
guess_mime_type(char *filename)
{
    char *suffix = strrchr(filename, '.');
    if (suffix == NULL)
        return "text/plain";

    if (!strcasecmp(suffix, ".html"))
        return "text/html";

    if (!strcasecmp(suffix, ".gif"))
        return "image/gif";

    if (!strcasecmp(suffix, ".png"))
        return "image/png";

    if (!strcasecmp(suffix, ".jpg"))
        return "image/jpeg";

    if (!strcasecmp(suffix, ".js"))
        return "text/javascript";

    if (!strcasecmp(suffix, ".css"))
        return "text/css";

    if (!strcasecmp(suffix, ".svg"))
        return "image/svg+xml";

    if (!strcasecmp(suffix, ".mp4"))
        return "video/mp4";

    /* hint: you need to add support for (at least) .css, .svg, and .mp4
     * You can grep /etc/mime.types for the correct types */
    return "text/plain";
}

/* Handle HTTP transaction for static files. */
static bool
handle_static_asset(struct http_transaction *ta, char *basedir)
{
    char fname[PATH_MAX];
    char fallback_fname[PATH_MAX];

    assert (basedir != NULL || !!!"No base directory. Did you specify -R?");
    char *req_path = bufio_offset2ptr(ta->client->bufio, ta->req_path);

    if (strcmp(req_path, "/") == 0) {
        snprintf(fname, sizeof fname, "%s/index.html", basedir);
    } 
    else {
        snprintf(fname, sizeof fname, "%s%s.html", basedir, req_path);
        if (access(fname, R_OK) == -1) {
            if (errno == EACCES) {
                return send_error(ta, HTTP_PERMISSION_DENIED, "Permission denied.");
            }
            snprintf(fname, sizeof fname, "%s%s", basedir, req_path);
            if (access(fname, R_OK) == -1) {
                snprintf(fallback_fname, sizeof fallback_fname, "%s/200.html", basedir);
                if (access(fallback_fname, R_OK) == 0) {
                    snprintf(fname, sizeof fname, "%s/200.html", basedir);
                } else {
                    return send_not_found(ta);
                }
            }
        }
    }

    // Determine file size
    struct stat st;
    int rc = stat(fname, &st);

    if (rc == -1) {
        return send_error(ta, HTTP_INTERNAL_ERROR, "Could not stat file.");
    }

    if (S_ISDIR(st.st_mode)) {
        // Fallback to /200.html for directory requests if it exists
        snprintf(fallback_fname, sizeof fallback_fname, "%s/200.html", basedir);
        if (access(fallback_fname, R_OK) == 0) {
            snprintf(fname, sizeof fname, "%s/200.html", basedir);
        } else {
            return send_not_found(ta);
        }
    }

    int filefd = open(fname, O_RDONLY);
    if (filefd == -1) {
        return send_not_found(ta);
    }

    ta->resp_status = HTTP_OK;
    http_add_header(&ta->resp_headers, "Content-Type", "%s", guess_mime_type(fname));
    http_add_header(&ta->resp_headers, "Accept-Ranges", "bytes");
    off_t from = 0, to = st.st_size - 1;

    off_t content_length = to + 1 - from;
    if (!ta->has_range)
    {
        add_content_length(&ta->resp_headers, content_length);
        ta->resp_status = HTTP_OK;
    }
    else {

        if (ta->range_end == -1 || ta->range_end >= st.st_size) {
            ta->range_end = st.st_size - 1;  // Adjust end if out of bounds or unspecified
        }

        from = ta->range_start;
        to = ta->range_end;
        content_length = to - from + 1;  // Calculate the length of the range
        add_content_length(&ta->resp_headers, content_length);
        lseek(filefd, from, SEEK_SET);   // Position file pointer to the start of the range

        ta->resp_status = HTTP_PARTIAL_CONTENT;
        http_add_header(&ta->resp_headers, "Content-Range", "bytes %ld-%ld/%ld", from, to, st.st_size);
    }

    bool success = send_response_header(ta);
    if (!success)
        goto out;

    // sendfile may send fewer bytes than requested, hence the loop
    while (success && from <= to) {
        success = bufio_sendfile(ta->client->bufio, filefd, &from, to + 1 - from) > 0;
    }

out:
    close(filefd);
    return success;
}

static bool handle_video_list(struct http_transaction *ta, const char *base_dir) {
    DIR *dir = opendir(base_dir);
    if (dir == NULL) {
        return send_error(ta, HTTP_INTERNAL_ERROR, "Could not open directory.");
    }

    struct dirent *entry;
    json_t *video_array = json_array();

    while ((entry = readdir(dir)) != NULL) {
        if (strstr(entry->d_name, ".mp4") != NULL) {
            struct stat st;
            char full_path[PATH_MAX];
            snprintf(full_path, sizeof(full_path), "%s/%s", base_dir, entry->d_name);

            if (stat(full_path, &st) == 0) {
                json_t *video_obj = json_object();
                json_object_set_new(video_obj, "name", json_string(entry->d_name));
                json_object_set_new(video_obj, "size", json_integer(st.st_size));
                json_array_append_new(video_array, video_obj);
            }
        }
    }

    closedir(dir);

    char *video_list = json_dumps(video_array, 0);
    json_decref(video_array);

    ta->resp_status = HTTP_OK;
    http_add_header(&ta->resp_headers, "Content-Type", "application/json");
    buffer_appends(&ta->resp_body, video_list);

    free(video_list);

    return send_response(ta);
}

/* Cookie creation method */
static char*
create_token(char* user) {
    jwt_t *mytoken;

    int rc = jwt_new(&mytoken);
    if (rc)
        die("jwt_new", rc);

    rc = jwt_add_grant(mytoken, "sub", user);
    if (rc)
        die("jwt_add_grant sub", rc);

    time_t now = time(NULL);
    rc = jwt_add_grant_int(mytoken, "iat", now);
    if (rc)
        die("jwt_add_grant iat", rc);

    rc = jwt_add_grant_int(mytoken, "exp", now + 2);
    if (rc)
        die("jwt_add_grant exp", rc);

    char *key = getenv("SECRET");
    if (key == NULL) {
        die("getenv", EINVAL);
    }
    rc = jwt_set_alg(mytoken, JWT_ALG_HS256, (unsigned char *)key, strlen(key));
    if (rc)
        die("jwt_set_alg", rc);

    char *encoded = jwt_encode_str(mytoken);
    if (encoded == NULL)
        die("jwt_encode_str", ENOMEM);

    jwt_free(mytoken);
    return encoded;
}

static char* return_claims(const char* jwt_encoded) {
    if (!jwt_encoded) return strdup("{}");

    jwt_t *jwt = NULL;
    int ret = jwt_decode(&jwt, jwt_encoded, (const unsigned char*)getenv("SECRET"), strlen(getenv("SECRET")));
    if (ret != 0 || !jwt) {
        // If the token cannot be decoded, return an empty JSON object
        if (jwt) jwt_free(jwt);
        return strdup("{}");
    }

    // Extract claims
    int exp = jwt_get_grant_int(jwt, "exp");
    int iat = jwt_get_grant_int(jwt, "iat");
    const char* sub = jwt_get_grant(jwt, "sub");

    // Create JSON object
    json_t *root = json_object();
    if (exp) json_object_set_new(root, "exp", json_integer(exp));
    if (iat) json_object_set_new(root, "iat", json_integer(iat));
    if (sub) json_object_set_new(root, "sub", json_string(sub));

    // Convert JSON object to string
    char *claims_str = json_dumps(root, JSON_ENCODE_ANY);
    json_decref(root);
    jwt_free(jwt);

    return claims_str ? claims_str : strdup("{}");
}

static bool
handle_api(struct http_transaction *ta) {
    char *req_path = bufio_offset2ptr(ta->client->bufio, ta->req_path); 

    if (strcmp(req_path, "/api/login") == 0) {
        if (ta->req_method == HTTP_POST) {
            json_error_t error;
            char *body = bufio_offset2ptr(ta->client->bufio, ta->req_body);

            json_t *jbody = json_loadb(body, ta->req_content_len, 0, &error);
            if (!jbody) {
                return send_error(ta, HTTP_BAD_REQUEST, "Invalid JSON data: %s", error.text);
            }

            const char *env_user = getenv("USER_NAME");
            const char *env_pass = getenv("USER_PASS");


            if (!env_user || !env_pass) {
                json_decref(jbody);
                return send_error(ta, HTTP_INTERNAL_ERROR, "Server configuration error.");
            }

            const char *temp_user = json_string_value(json_object_get(jbody, "username"));
            const char *temp_pass = json_string_value(json_object_get(jbody, "password"));
            char *local_user = temp_user ? strdup(temp_user) : NULL;
            char *local_pass = temp_pass ? strdup(temp_pass) : NULL;
            json_decref(jbody);     

            if (!local_user || !local_pass || strcmp(local_user, env_user) != 0 || strcmp(local_pass, env_pass) != 0) {
                return send_error(ta, HTTP_PERMISSION_DENIED, "Authentication failed.");
            }

            char *token = create_token(local_user);
            if (!token) {
                return send_error(ta, HTTP_INTERNAL_ERROR, "Failed to create authentication token.");
            }

            json_t *claims = json_object();
            json_object_set_new(claims, "exp", json_integer(time(NULL) + 2 * 86400));
            json_object_set_new(claims, "iat", json_integer(time(NULL)));
            json_object_set_new(claims, "sub", json_string(local_user));

            char *claims_str = json_dumps(claims, JSON_ENCODE_ANY);
            json_decref(claims);

            http_add_header(&ta->resp_headers, "Content-Type", "application/json");
            http_add_header(&ta->resp_headers, "Set-Cookie", "auth_jwt_token=%s; Max-Age=2; HttpOnly; SameSite=Lax; Path=/", token);
            ta->resp_status = HTTP_OK;
            buffer_appends(&ta->resp_body, claims_str);
            free(claims_str);
            free(token);
            return send_response(ta);
        }
        else if (ta->req_method == HTTP_GET) {
            http_add_header(&ta->resp_headers, "Content-Type", "application/json");
            if (ta->client->authenticated) {
                char* claims = return_claims(ta->jwt_token);  // Assuming you store JWT in the client struct
                if (claims) {
                    buffer_appends(&ta->resp_body, claims);
                    free(claims); // if return_claims dynamically allocates the claims string
                } else {
                    buffer_appends(&ta->resp_body, "{}");
                }
            } else {
                buffer_appends(&ta->resp_body, "{}");
            }
            ta->resp_status = HTTP_OK;
            return send_response(ta);
        } else {
            
        ta->resp_status = HTTP_NOT_IMPLEMENTED;
        http_add_header(&ta->resp_headers, "Content-Type", "application/json");
        buffer_appends(&ta->resp_body, "{}");
        return send_response(ta);
        }
    }
    else if (strcmp(req_path, "/api/video") == 0) {
        return handle_video_list(ta, server_root);
    }
    return send_error(ta, HTTP_NOT_FOUND, "API endpoint not found");
}

/* Set up an http client, associating it with a bufio buffer. */
void 
http_setup_client(struct http_client *self, struct bufio *bufio)
{
    self->bufio = bufio;
    self->authenticated = false;
}

static bool validate_token(const char *jwt_token) {

    jwt_t *jwt = NULL;
    int ret = jwt_decode(&jwt, jwt_token, (const unsigned char*)getenv("SECRET"), strlen(getenv("SECRET")));
    if (ret != 0) return false; 

    time_t now = time(NULL);
    time_t exp = jwt_get_grant_int(jwt, "exp");
    if (now >= exp) {
        jwt_free(jwt);
        return false;
    }

    jwt_free(jwt);
    return true; 
}
/* Handle a single HTTP transaction.  Returns true on success. */
bool
http_handle_transaction(struct http_client *self)
{
    struct http_transaction ta;
    memset(&ta, 0, sizeof ta);
    ta.client = self;

    if (!http_parse_request(&ta))
        return false;

    if (!http_process_headers(&ta))
        return false;

    if (ta.req_content_len > 0) {
        int rc = bufio_read(self->bufio, ta.req_content_len, &ta.req_body);
        if (rc != ta.req_content_len)
            return false;

        // To see the body, use this:
        // char *body = bufio_offset2ptr(ta.client->bufio, ta.req_body);
        // hexdump(body, ta.req_content_len);
    }

    buffer_init(&ta.resp_headers, 1024);
    http_add_header(&ta.resp_headers, "Server", "CS3214-Personal-Server");
    buffer_init(&ta.resp_body, 0);

    bool rc = false;
    char *req_path = bufio_offset2ptr(ta.client->bufio, ta.req_path);
    if (strstr(req_path, ".."))
    {
        send_error(&ta, HTTP_BAD_REQUEST, "Bad Request");
    }
    if (STARTS_WITH(req_path, "/api")) {
        rc = handle_api(&ta);
    } else
    if (STARTS_WITH(req_path, "/private")) {
        if (ta.jwt_token && validate_token(ta.jwt_token)) {
            rc = handle_static_asset(&ta, server_root);
        } else {
            return send_error(&ta, HTTP_PERMISSION_DENIED, "Permission denied.");
        }
    } else if (STARTS_WITH(req_path, "/login")) {
        send_error(&ta, HTTP_NOT_FOUND, "API Endpoint not Found.");
    } 
    else {
        rc = handle_static_asset(&ta, server_root);
    }

    buffer_delete(&ta.resp_headers);
    buffer_delete(&ta.resp_body);

    return ta.req_version == HTTP_1_1 && rc;
}
