-------------------------------------------------
 *  Chung-An, Chiu 12172213 
 *  Project 0
 *  Oct 5, 2017
-------------------------------------------------

This is a Java program that helps you clear the
white spaces, tabs and commnets in your text file.

-------------------------------------------------

0)Environment set up
(If you already have Java environment set up, you
may jump to the 1st section below.)

--First, you must have the Java compiler,Java 
Development Kit (JDK),which can be downloaded from 
Oracle,

http://www.oracle.com/technetwork/java/javase/downloads/index.html 

For Max/Linux users, you can use apt/apt-get
to download it,

sudo apt-get update | sudo apt-get install default-jdk

The command is somewhat like this. Could be different
owing to your distribution.

--Second, you need a Java Runtime Environment(JRE)
to execute the java program.

In command line, type "java -version" to see if you 
have installed it yet. If not, download it from
Oracle website,

http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html

Then, setup the path so that your system recognizes
your java command.

https://docs.oracle.com/javase/tutorial/essential/environment/paths.html

-------------------------------------------------

1)Compile the code

At command line, change your directory to the "src"
folder, there sould be a file "ClearFile.java".
To compile the code, type in

javac ClearFile.java

Afterwards, you may see the "ClearFile.class" file.

-------------------------------------------------

2)Execute the code
--At the same directory, simply type in,

java ClearFile <your-file-name.in>

After the execution there will be a <your-file-name.out>
file in the same directory, which has all whitespaces/tabs
clear out.

--Execute with "no-comments" argument

java ClearFile <your-file-name.in> no-comments

By doing so, the comments,"//", in your .out file will 
be deliminated.

-------------------------------------------------

If you have any questions, or you find out a defect
of the program, please email me with your circumstances
and possibly your input file as well at

chungan@uchicago.edu

I appreciate your help.

Enjoy!

-------------------------------------------------
