// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.


(LOOP)

@SCREEN
D=A
@addr
M=D     //store screen's address in addr

@8192   //512 col * 255 row / 16 bit chunk
D=A
@0
M=D
D=M
@n
M=D     //n=RAM[0]

@i
M=0     //i=0 

@KBD
D=M
@WHITE
D;JEQ   //if no input(D=0), white
@BLACK
D;JGT   //if no input(D>0), black 

(WHITE)
@i
D=M
@n
D=D-M
@LOOP
D;JEQ  //if i=n goto LOOP

@addr
A=M
M=0

@i
M=M+1
@1
D=A
@addr
M=M+D
@WHITE
0;JMP  //go to WHITE

(BLACK)
@i
D=M
@n
D=D-M
@LOOP
D;JEQ  //if i=n goto LOOP

@addr
A=M
M=-1

@i
M=M+1
@1
D=A
@addr
M=M+D
@BLACK
0;JMP  //go to BLACK


//No matter what, continue
@LOOP
0;JMP
