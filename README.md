# TAIMA

This is the backend for my other project [TAIM](https://github.com/doompickaxe/Taim).

This basically is a program to save and manage the worklog of employees or projects.
 
It is written in Kotlin with the Ktor project.
You can decide if you use sqlite or postgres for the database.

## Instructions
Please have a look into src/main/resources/application.conf and configure the database and auth.

If you want to use the application you won't have any rights to log in or do anything.

When the application cannot find any user in the database, it will create one with admin rights.
Since I suppose you have access rights to the database, just change the email address of that user
to your own and then you are admin of this application.

## More details
You can have configurations for each person in the database.
A configuration consists of how long a person has to work on which days in a given period.
In this period, also the amount of days for vacation are stored, so I suggest that the longest period
is a year to make your own life easier.

The person then can log on which days they have worked for how long, or if they had a reason not
to work(ill or vacation).

## Still to be done
* Reports