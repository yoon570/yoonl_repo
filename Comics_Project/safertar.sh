#! /bin/bash
#
#  Runs tar command to create an uncompressed archive file, but warns
#  if the (apparent) target tar file already exists, and asks user to
#  confirm that they way to overwrite the existing file.
#
#  This script has been used, but not extensively.  There is no warranty
#  as to its correctness, and use of the script is at your own risk.
#
#
############################################# fn to check for tar file
#                 param1:  name of file to be checked
isTar() {

   mimeType=`file -b --mime-type $1`
   [[ $mimeType == "application/x-tar" ]]
}

#  Check for command-line parameters; there must be at least two:
#     - name of tar file to be created/overwritten
#     - name of at least one file to place in archive
#
   if [[ $# -lt 2 ]]; then
      echo "You must specify a target name and at least one file to be archived."
      exit -1
   fi
#
#  Check whether the first parameter already exists; if so, warn and
#  confirm it's to be overwritten.
#
   if [[ -e $1 ]]; then
      echo "$1 already exists"
      isTar $1
      if [[ $? -ne 0 ]]; then
         echo "$1 IS NOT a tar file"
      else
         echo "$1 IS a tar file"
      fi
      echo "Do you want to overwrite (destroy) $1?  Y/N"
      read response
      echo "You said: $response"
      if [[ $response == "N" ]] || [[ $response == "n" ]]; then
         exit 0;
      elif [[ $response != "Y" ]] && [[ $response != "y" ]]; then
         echo "I don't understand $response... giving up."
         exit -2
      else
         echo "OK, here goes..."
      fi
   fi
#
#  Either first parameter does not exist, or user has confirmed it is
#  to be overwritten... so execute the tar operation.
#
   echo "Executing tar cvf $@ ..."
   tar cvf $@
#
exit 0
