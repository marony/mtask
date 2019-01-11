#!/bin/sh

cp application.conf.template application.conf

IFS=$'\n'
for line in `cat sensitive.txt`
do
    echo $line | awk -F',' '{ system("sed -e \"s/"$1"/"$2"/g\" application.conf") }' > application.conf.new
    rm application.conf
    mv application.conf.new application.conf
done
