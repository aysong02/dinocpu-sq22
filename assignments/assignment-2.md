---
Authors: Jason Lowe-Power, Filipe Eduardo Borges
Editor: Justin Perona, Julian Angeles, Maryam Babaie, Christopher Nitta, Andrew Song
Title: DINO CPU Assignment 2
---

# DINO CPU Assignment 2

Originally from ECS 154B Lab 2, Winter 2019.

Modified for ECS 154B Lab 2, Spring 2022.

# Table of Contents

* [Introduction](#introduction)
  * [Updating the DINO CPU code](#updating-the-dino-cpu-code)
  * [How this assignment is written](#how-this-assignment-is-written)
  * [Goals](#goals)
* [Single cycle CPU design](#single-cycle-cpu-design)
* [Control unit overview](#control-unit-overview)
* [Part 0: NextPC unit overview](#part-0-nextpc-unit-overview)
* [Part I: R-types](#part-i-r-types)
  * [R-type instruction details](#r-type-instruction-details)
  * [Testing the R-types](#testing-the-r-types)
* [Part II: I-types](#part-ii-i-types)
  * [I-type instruction details](#i-type-instruction-details)
  * [Testing the I-types](#testing-the-i-types)
* [Part III: `lw`](#part-iii-lw)
  * [`lw` instruction details](#lw-instruction-details)
  * [Testing `lw`](#testing-lw)
* [Part IV: U-types](#part-iv-u-types)
  * [`lui` instruction details](#lui-instruction-details)
  * [`auipc` instruction details](#auipc-instruction-details)
  * [Testing the U-types](#testing-the-u-types)
* [Part V: `sw`](#part-v-sw)
  * [`sw` instruction details](#sw-instruction-details)
  * [Testing `sw`](#testing-sw)
* [Part VI: Other memory instructions](#part-vi-other-memory-instructions)
  * [Other memory instruction details](#other-memory-instruction-details)
  * [Testing the other memory instructions](#testing-the-other-memory-instructions)
* [Part VII: Branch instructions](#part-vii-branch-instructions)
  * [Branch instruction details](#branch-instruction-details)
  * [Updating your NextPC unit for branch instructions](#updating-your-NextPC-unit-for-branch-instructions)
    * [Testing your NextPC unit](#testing-your-nextpc-unit)
* [Part VIII: `jal`](#part-viii-jal)
  * [`jal` instruction details](#jal-instruction-details)
  * [Testing `jal`](#testing-jal)
* [Part IX: `jalr`](#part-ix-jalr)
  * [`jalr` instruction details](#jalr-instruction-details)
  * [Testing `jalr`](#testing-jalr)
* [Part X: Full applications](#part-x-full-applications)
  * [Testing full applications](#testing-full-applications)
* [Grading](#grading)
* [Submission](#submission)
  * [Code portion](#code-portion)
  * [Academic misconduct reminder](#academic-misconduct-reminder)
  * [Checklist](#checklist)
* [Hints](#hints)
  * [`printf` debugging](#printf-debugging)

# Introduction

![Cute Dino](../dino-128.png)

In the last assignment, you implemented the ALU control and incorporated it into the DINO CPU to test some bare-metal R-type RISC-V instructions.
In this assignment, you will implement the main control unit and NextPC unit and update the ALU control unit (if needed).
After implementing the individual components and successfully passing all individual component tests, you will combine these along with the other CPU components to complete the single-cycle DINO CPU.
The simple in-order CPU design is based closely on the CPU model in Patterson and Hennessy's Computer Organization and Design.

## Updating the DINO CPU code

The DINO CPU code must be updated before you can run each lab.
You should read up on [how to update your code](../documentation/updating-from-git.md) to get the assignment 2 template from GitHub.
We have made the following changes:
- The solution for cpu.scala for Assignment 1 is included
- The control unit includes the control signals for the R-type instructions

You can check out the main branch to get the template code for this lab.
If you want to use your solution from lab1 as a starting point, you can merge your commits with the `origin` main by running `git pull` or `git fetch; git merge origin/main`.

If you want to start over and use the provided solution, you can do the following (see [how to update your code](../documentation/updating-from-git.md) for more details):

```
git clone https://github.com/aysong02/dinocpu-sq22
cd dinocpu-sq22
```

## How this assignment is written

The goal of this assignment is to implement a single-cycle RISC-V CPU which can execute all of the RISC-V integer instructions.
Through the rest of this assignment, [Part I](#part-i-r-type) through [Part X](#part-x-full-applications), you will implement all of the RISC-V instructions, step by step.

If you prefer, you can simply skip to the end and implement all of the instructions at once, then run all of the tests for this assignment via the following command.
You will also use this command to test everything once you believe you're done.

```
sbt:dinocpu> test
```

We are making one major constraint on how you are implementing your CPU.
**You cannot modify the I/O for any module**.
We will be testing your control unit with our data path, and our data path with your control unit.
Therefore, you **must keep the exact same I/O**.
You will get errors on Gradescope (and thus no credit) if you modify the I/O.

## Goals

- Learn how to implement a control and data path in a single cycle CPU.
- Learn how different RISC-V instructions interact in the control and data path of a single cycle CPU.

# Single cycle CPU design

Below is a diagram of the single cycle DINO CPU.
This diagram includes all of the necessary data path wires and MUXes.
However, it is missing the control path wires.
This figure has all of the MUXes necessary, but does not show which control lines go to which MUX.
**Hint**: the comments in the code for the control unit give some hints on how to wire the design.

In this assignment, you will be implementing the data path shown in the figure below, implementing the control path for the DINO CPU, and wiring up the control path.
You can extend your work from [Project 1](./assignment-1.md), or you can take the updated code from [GitHub](https://github.com/aysong02/dinocpu-sq22.git/).
You will be implementing everything in the diagram in Chisel (the `cpu.scala` file only implements the R-type instructions), which includes the code for the MUXes.
Then, you will wire all of the components together.
You will also implement the [control unit](#control-unit-overview), NextPC unit and update the alu control unit.

**Important Notice:**
In order to get familiar with debugging your design using single stepper, ***we strongly encourage you to watch the tutorial video*** we have provided in the link below. You may have watched it while working on assignment1. As mentioned before, the videos were originally made for spring quarter 2020 (sq20). Just in case you wanted to use any command or text from these videos which contains 'sq20', you just need to convert it to 'sq22' to be applicable to your materials for the current quarter.

[DinoCPU - Debugging your implementation](https://video.ucdavis.edu/playlist/dedicated/0_8bwr1nkj/0_kv1v647d)

![Single cycle DINO CPU without control wires](single-cycle-no-control.png)

# Control unit overview

In this part, you will be implementing the main control unit in the CPU design.
The control unit is used to determine how to set the control lines for the functional units and the the multiplexers.

The control unit takes a single input, which is the 7-bit `opcode`.
From that input, it generates the 13 control signals listed below as output.


```scala
itype         true if we're working on an itype instruction
aluop         true for R-type and I-type, false otherwise
xsrc          source for the first ALU/nextpc input (0 is readdata1, 1 is pc)
ysrc          source for the second ALU/nextpc input (0 is readdata2 and 1 is immediate)
branch        true if branch
jal           true if a jal
jalr          true if a jalr instruction
plus4         true if ALU should add 4 to inputx
resultselect  false for result from alu, true for immediate
memop         00 if not using memory, 10 if reading, and 11 if writing
toreg         false for result from execute, true for data from memory
regwrite      true if writing to the register file
validinst     True if the instruction we're decoding is valid
```

The following table specifies the `opcode` format and the control signals to be generated for some of the instruction types.


| opcode  | opcode format | itype  | aluop | xsrc  | ysrc  | branch | jal   | jalr  | plus4 | resultselect | memop | toreg | regwrite | validinst |
|---------|---------------|--------|-------|-------|-------|--------|-------|-------|-------|--------------|-------|-------|----------|-----------|
| -       | default       | false  | false | false | false | false  | false | false | false | false        | 0     | false | false    | false     |
| 0000000 | invalid       | false  | false | false | false | false  | false | false | false | false        | 0     | false | false    | false     |
| 0110011 | R-type        | false  | true  | false | false | false  | false | false | false | false        | 0     | false | true     | true      |

We have given you the control signals for the R-type instructions.
You must fill in all of the other instructions in the table in `src/main/scala/components/control.scala`.
Notice how the third line of the table (under the `// R-type`) is an exact copy of the values in this table.

Given the input opcode, you must generate the correct control signals.
The template code from `src/main/scala/components/control.scala` is shown below.
You will fill in where it says *Your code goes here*.

```scala
// Control logic for the processor

package dinocpu.components

import chisel3._
import chisel3.util.{BitPat, ListLookup}

/**
 * Main control logic for our simple processor
 *
 * Input: opcode:     Opcode from instruction
 *
 * Output: itype         true if we're working on an itype instruction
 * Output: aluop         true for R-type and I-type, false otherwise
 * Output: xsrc          source for the first ALU/nextpc input (0 is readdata1, 1 is pc)
 * Output: ysrc          source for the second ALU/nextpc input (0 is readdata2 and 1 is immediate)
 * Output: branch        true if branch
 * Output: jal           true if a jal
 * Output: jalr          true if a jalr instruction
 * Output: plus4         true if ALU should add 4 to inputx
 * Output: resultselect  false for result from alu, true for immediate
 * Output: memop         00 if not using memory, 10 if reading, and 11 if writing
 * Output: toreg         false for result from execute, true for data from memory
 * Output: regwrite      true if writing to the register file
 * Output: validinst     True if the instruction we're decoding is valid
 *
 * For more information, see section 4.4 of Patterson and Hennessy.
 * This follows figure 4.22 somewhat.
 */

class Control extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))

    val itype        = Output(Bool())
    val aluop        = Output(Bool())
    val xsrc         = Output(Bool())
    val ysrc         = Output(Bool())
    val branch       = Output(Bool())
    val jal          = Output(Bool())
    val jalr         = Output(Bool())
    val plus4        = Output(Bool())
    val resultselect = Output(Bool())
    val memop        = Output(UInt(2.W))
    val toreg        = Output(Bool())
    val regwrite     = Output(Bool())
    val validinst    = Output(Bool())
  })

  val signals =
    ListLookup(io.opcode,
      /*default*/           List(false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B,      0.U,   false.B, false.B,  false.B),

      Array(              /*     itype,   aluop,   xsrc,    ysrc,    branch,  jal,     jalr     plus4,   resultselect, memop, toreg,   regwrite, validinst */
      // R-format
      BitPat("b0110011") -> List(false.B, true.B,  false.B, false.B, false.B, false.B, false.B, false.B, false.B,      0.U,   false.B, true.B,   true.B),

      // Your code goes here.
      // Remember to make sure to have commas at the end of each line, except for the last one.

      ) // Array
    ) // ListLookup

  io.itype        := signals(0)
  io.aluop        := signals(1)
  io.xsrc         := signals(2)
  io.ysrc         := signals(3)
  io.branch       := signals(4)
  io.jal          := signals(5)
  io.jalr         := signals(6)
  io.plus4        := signals(7)
  io.resultselect := signals(8)
  io.memop        := signals(9)
  io.toreg        := signals(10)
  io.regwrite     := signals(11)
  io.validinst    := signals(12)
}
```

In this code, you can see that the `ListLookup` looks very similar to the table above.
You will be filling in the rest of the lines of this table.
As you work through each of the parts below, you will be adding a line to the table.
You will have one line for each type of instruction (i.e., each unique opcode that for the instructions you are implementing).

You will not need to use the `validinst` signal.
It is used for exceptions and other system-related instructions that we are not implementing in this assignment.

**Important: DO NOT MODIFY THE I/O.**
You do not need to modify any other code in this file other than the `signals` table!

**Important: Any don't care lines should be set to 0!**
There may be some cases where some control signals could be 1 or 0 (i.e., you don't care what the value is).
You are *required* to set these lines to 0 for this assignment.
If you do not set these lines to 0, you will not pass the control unit test on Gradescope.


# Part 0: NextPC unit overview

In the last assignment, when you were required to run your CPU for multiple cycles, you used a simple adder to generate the next value for the pc which was simply pc+4 in all cases. In this assignment you will implement a much smarter unit, NextPC, which is responsible for the next value that must be assigned to the pc for the next cycle.

The NextPC unit recieves eight inputs:

* `branch`, `jal`, `jalr`, which are boolean and come from the control unit.
* `eqf` and `ltf`, which are boolean and come from the ALU.
* `funct3`, which comes from the instruction.
* `pc`, which is the pc.
* `imm`, which comes from the Immediate Generator unit.

The NextPC unit generates two outputs:

* `nextpc`, which is 32 bits unsigned integer value that must be assigned to the pc for the next cycle.
* `taken`, which is boolean showing if a branch or jump result is taken or not.

Given the inputs, you must generate the correct value for nextpc and taken. The template code from `src/main/scala/components/nextpc.scala` is shown below.

```scala
// Logic to calculate the next pc

package dinocpu.components

import chisel3._

/**
 * Next PC unit. This takes various inputs and outputs the next address of the next instruction.
 *
 * Input: branch         true if executing a branch instruction
 * Input: jal            true if executing a jal
 * Input: jalr           true if executing a jalr
 * Input: eqf            true if inputx == inputy in ALU
 * Input: ltf            true if inputx < input y in ALU
 * Input: funct3         the funct3 from the instruction
 * Input: pc             the *current* program counter for this instruction
 * Input: imm            the sign-extended immediate
 *
 * Output: nextpc        the address of the next instruction
 * Output: taken         true if the next pc is not pc+4
 *
 */
class NextPC extends Module {
  val io = IO(new Bundle {
    val branch  = Input(Bool())
    val jal     = Input(Bool())
    val jalr    = Input(Bool())
    val eqf     = Input(Bool())
    val ltf     = Input(Bool())
    val funct3  = Input(UInt(3.W))
    val pc      = Input(UInt(32.W))
    val imm     = Input(UInt(32.W))

    val nextpc  = Output(UInt(32.W))
    val taken   = Output(Bool())
  })

  io.nextpc := io.pc + 4.U
  io.taken  := false.B

  // Your code goes here
}
```
In this template, the outputs, `nextpc` and `taken`, are set to always be pc+4 and false, respectively.

Before starting Part I, you must remove the parts related to pc+4 and replace it with an instance of NextPC unit and create proper wire connections for it. For this purpose, you must update `src/main/scala/single-cycle/cpu.scala`. In the next sections, you will gradually complete the body of NextPC unit.

# Part I: R-types

In the last assignment, you implemented a subset of the RISC-V data path for just R-type instructions.
This did not require a control unit since there were no need for extra MUXes.
However, in this assignment, you will be implementing the rest of the RISC-V instructions, so you will need to use the control unit.

The first step is to hook up the control unit and get the R-type instructions working again.
You shouldn't have to change all that much code in `cpu.scala` from the first assignment after you applying changes regarding [NextPC unit](#part-0-nextpc-unit-overview).
All you have to do is to hook up the `opcode` to the input of the control unit and its output `aluop` and `itype` to the alu control unit.
We have already implemented the R-type control logic for you.
You can also use the appropriate signals generated from the control unit (e.g., `regwrite`) to drive your data path.

## R-type instruction details

The following table shows how an R-type instruction is laid out:

| 31-25  | 24-20 | 19-15 | 14-12   | 11-7 | 6-0     | Name   |
|--------|-------|-------|---------|------|---------|--------|
| funct7 | rs2   | rs1   | funct3  | rd   | 0110011 | R-type |

Each instruction has the following effect.
`<op>` is specified by the `funct3` and `funct7` fields.
`R[x]` means the value stored in register x.

```
R[rd] = R[rs1] <op> R[rs2]
```

## Testing the R-types

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleRTypeTesterLab2
```

# Part II: I-types

Next, you will implement the I-type instructions.
These are mostly the same as the the R-types, except that the second operand comes from the immediate value contained within the instruction, rather than another register.

**Important**: The immediate generator will produce the shifted and sign extended value!
You do not need to shift the immediate value outside of the immediate generator.

To implement the I-types, you should first extend the table in `control.scala`.
Then you can add the appropriate MUXes to the CPU (in `cpu.scala`) and wire the control signals to those MUXes.
**HINT**: You only need one extra MUX, compared to your R-type-only design.

In this section, you will (likely) also have to update your ALU control unit.
In assignment 1, we ignored the `aluop` and `itype` inputs on the ALU control unit.
Now that we are running the I-type instructions, we have to make sure that when we're executing I-type instructions the ALU control unit ignores the `funct7` bits.
For I-type instructions, these bits are part of the immediate field!

## I-type instruction details

The following table shows how an I-type instruction is laid out:

|31-20      | 19-15 | 14-12  | 11-7 | 6-0     | Name   |
|-----------|-------|--------|------|---------|--------|
| imm[11:0] | rs1   | funct3 | rd   | 0010011 | I-type |

Each instruction has the following effect.
`<op>` is specified by the `funct3` field.

```
R[rd] = R[rs1] <op> immediate
```

## Testing the I-types

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleITypeTesterLab2
```

# Part III: `lw`

Next, we will implement the `lw` instruction.
Officially, this is a I-type instruction, so you shouldn't have to make too many modifications to your data path.

As with the previous parts, first update your control unit to assert the necessary control signals for the `lw` instruction, then modify your CPU data path to add the necessary MUXes and wire up your control.
For this part, you will have to think about how this instruction uses the ALU.
You will also need to incorporate the data memory into your data path, starting with this instruction.

## Data memory port I/O
The data memory port I/O is not as simple as the I/O for other modules.
It's built to be modular to allow different kinds of memories to be used with your CPU design.
We are planning to explore this further in Lab 4.
If you want to see the details, you can find them in the [mem-port-io.scala](https://github.com/jlpteaching/dinocpu/blob/main/src/main/scala/memory/memory-port-io.scala) file.

The I/O for the data memory port is shown below.
Don't forget that the instruction and data memory ports look weird to use.
You have to say `io.dmem`, which seems backwards.
For this assignment, you can ignore the good and ready signals since memory will respond to the request in the same cycle.

```
Input:  address, the address of a piece of data in memory.
Input:  writedata, valid interface for the data to write to the address
Input:  valid, true when the address (and writedata during a write) specified is valid
Input:  memread, true if we are reading from memory
Input:  memwrite, true if we are writing to memory
Input:  maskmode, mode to mask the result. 0 means byte, 1 means halfword, 2 means word
Input:  sext, true if we should sign extend the result

Output: readdata, the data read and sign extended
Output: good, true when memory is responding with a piece of data
Output: ready, true when the memory is ready to accept another request
```

## `lw` instruction details

The following table shows how the `lw` instruction is laid out:

| 31-20     | 19-15 | 14-12 | 11-7 | 6-0     | Name   |
|-----------|-------|-------|------|---------|--------|
| imm[11:0] | rs1   | 010   | rd   | 0000011 | lw     |

`lw` stands for "load word".
The instruction has the following effect.
`M[x]` means the value of memory at location x.

```
R[rd] = M[R[rs1] + immediate]
```

## Testing `lw`

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleLoadTesterLab2
```

# Part IV: U-types

U-types are another type of instruction that look similar to the I-types.
There are two of them you need to implement, described below.

## `lui` instruction details

The following table shows how the `lui` instruction is laid out.


| 31-12      | 11-7 | 6-0     | Name   |
|------------|------|---------|--------|
| imm[31:12] | rd   | 0110111 | lui    |

`lui` stands for "load upper immediate."
The instruction has the following effect.
As in C and C++, the `<<` operator means bit shift left by the number specified.

```
R[rd] = imm << 12
```

**Important**: The immediate generator will produce the shifted and sign extended value!
You do not need to shift the immediate value outside of the immediate generator.

Use the diagram as a hint on how to modify your data path and control unit for this instruction.
There are multiple different ways to implement this instruction (last year, we used a different design), so be careful to follow the diagram above!

## `auipc` instruction details

The following table shows how the `auipc` instruction is laid out.

| 31-12      | 11-7 | 6-0     | Name   |
|------------|------|---------|--------|
| imm[31:12] | rd   | 0010111 | auipc  |

`auipc` stands for "add upper immediate to pc."
The instruction has the following effect.

```
R[rd] = pc + imm << 12
```
Like previous part, use the diagram as a hint on how to modify your data path and control unit for this instruction.

## Testing the U-types

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleUTypeTesterLab2
```

# Part V: `sw`

`sw` is similar to `lw` in function, and looks similar to an I-type.
You'll need to think about how to implement the changes needed for the data memory.

## `sw` instruction details

The following table shows how the `sw` instruction is laid out.

| 31-25     | 24-20 | 19-15 | 14-12 | 11-7     | 6-0     | Name   |
|-----------|-------|-------|-------|----------|---------|--------|
| imm[11:5] | rs2   | rs1   | 010   | imm[4:0] | 0100011 | sw     |

`sw` stands for "store word."
The instruction has the following effect.
(Careful, while this looks similar to `lw`, it has a very different effect!)

```
M[R[rs1] + immediate] = R[rs2]
```

## Testing `sw`

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleStoreTesterLab2
```

# Part VI: Other memory instructions

We now move on to the other memory instructions.
Make sure your `lw` and `sw` instructions work before moving on to this part.

## Other memory instruction details

The following table show how the other memory instructions are laid out.
`lw` and `sw` are included again as a reference.

| 31-25     | 24-20    | 19-15 | 14-12 | 11-7     | 6-0     | Name   |
|-----------|----------|-------|-------|----------|---------|--------|
| imm[11:5] | imm[4:0] | rs1   | 000   | rd       | 0000011 | lb     |
| imm[11:5] | imm[4:0] | rs1   | 001   | rd       | 0000011 | lh     |
| imm[11:5] | imm[4:0] | rs1   | 010   | rd       | 0000011 | lw     |
| imm[11:5] | imm[4:0] | rs1   | 100   | rd       | 0000011 | lbu    |
| imm[11:5] | imm[4:0] | rs1   | 101   | rd       | 0000011 | lhu    |
| imm[11:5] | rs2      | rs1   | 000   | imm[4:0] | 0100011 | sb     |
| imm[11:5] | rs2      | rs1   | 001   | imm[4:0] | 0100011 | sh     |
| imm[11:5] | rs2      | rs1   | 010   | imm[4:0] | 0100011 | sw     |

`l` and `s` mean "load" and "store," as mentioned previously.
`b` means a "byte" (8 bits), while `h` means "half" of a word (16 bits).
`u` means "unsigned."

The instructions have the following effects.
`sext(x)` stands for "sign-extend x."
As in C and C++, `&` stands for bit-wise AND.

**Hint**: The data memory port has `mask` and `sext` (sign extend) inputs.
You do not need to mask or sign extend the result outside of the data memory port.
The data memory port takes care of these details for you.

```
lb:  R[rd] = sext(M[R[rs1] + immediate] & 0xff)
lh:  R[rd] = sext(M[R[rs1] + immediate] & 0xffff)
lw:  R[rd] = M[R[rs1] + immediate]
lbu: R[rd] = M[R[rs1] + immediate] & 0xff
lhu: R[rd] = M[R[rs1] + immediate] & 0xffff
sw:  M[R[rs1] + immediate] = R[rs2]
sb:  M[R[rs1] + immediate] = R[rs2] & 0xff
sh:  M[R[rs1] + immediate] = R[rs2] & 0xffff
```

## Testing the other memory instructions

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleLoadStoreTesterLab2
```

# Part VII: Branch instructions

This part is a little more involved than the previous instructions.
First, you will update the NextPC unit.
Then, you will update other necessary modules (e.g. control unit, ALU control if needed, etc) and then wire up the other necessary MUXes.

## Branch instruction details

The following table show how the branch instructions are laid out.

| imm[12, 10:5] | rs2   | rs1   | funct3 | imm[4:1, 11] | opcode  | Name   |
|---------------|-------|-------|--------|--------------|---------|--------|
| 31-25         | 24-20 | 19-15 | 14-12  | 11-7         | 6-0     |        |
| imm[12, 10:5] | rs2   | rs1   | 000    | imm[4:1, 11] | 1100011 |  beq   |
| imm[12, 10:5] | rs2   | rs1   | 001    | imm[4:1, 11] | 1100011 |  bne   |
| imm[12, 10:5] | rs2   | rs1   | 100    | imm[4:1, 11] | 1100011 |  blt   |
| imm[12, 10:5] | rs2   | rs1   | 101    | imm[4:1, 11] | 1100011 |  bge   |
| imm[12, 10:5] | rs2   | rs1   | 110    | imm[4:1, 11] | 1100011 |  bltu  |
| imm[12, 10:5] | rs2   | rs1   | 111    | imm[4:1, 11] | 1100011 |  bgeu  |

`b` here stands for branch.
`u` again means "unsigned."
The other portion of the mnemonics stand for the operation, either:

* `eq` for equals
* `ne` for not equals
* `lt` for less than
* `ge` for greater than or equal to

The instructions have the following effects.
The operation is given by `funct3` (see above).

```
if (R[rs1] <op> R[rs2])
  pc = pc + immediate
else
  pc = pc + 4
```

## Updating your NextPC unit for branch instructions

In this part you will be updating the NextPC component to account for branch
instructions. Similar to the CPU implementation in the book, the NextPC will compute
whether or not a branch is taken (outputting its result in `taken`, true if the branch is taken and false if the branch is not taken).

You must take the RISC-V ISA specification and implement the proper control to choose the right type of branch test.
You can find the specification in the following places:

* [the table above](#branch-instruction-details), copied from the RISC-V User-level ISA Specification v2.2, page 104
* Chapter 2 of the Specification
* Chapter 2 of the RISC-V reader
* in the front of the Computer Organization and Design book

You must now extend NextPC module with additional control to generate correct value for `nextpc` and correct result for `taken`.
As a helpful tip, it's important to remember that the `funct3` wire helps differentiate between different branch instructions.

See [the Chisel getting started guide](../documentation/chisel-notes/getting-started.md) for examples.
You may also find the [Chisel cheat sheet](https://chisel.eecs.berkeley.edu/2.2.0/chisel-cheatsheet.pdf) helpful.

**HINT:** Use Chisel's `when` / `elsewhen` / `otherwise`, or `MuxCase` syntax.
You can also use normal operators, such as `<`, `>`, `===`, `=/=`, etc.

**Important Note:** In this assignment you will not use the `taken` output of NextPC unit anywhere in your single cycle CPU. However, we test if your NextPC unit generates correct value for it. You will utilize `taken` in the next assignment.

## Testing your NextPC unit

We have updated the tests for your NextPC unit. The tests, along with the other lab2 tests, are in `src/test/scala/labs/Lab2Test.scala`.

In this part of the assignment, you only need to run the NextPC unit tests.
To run just these tests, you can use the sbt comand `testOnly`, as demonstrated below.

```
sbt:dinocpu> testOnly dinocpu.NextPCBranchTesterLab2
```

## Implementing branch instructions

Next, you need to wire the `nextpc` output from the NextPC unit into the data path.
You can follow the diagram given in [the single cycle CPU design section](#single-cycle-cpu-design).

## Testing the branch instructions

You can run the tests for the branch instructions with the following command.

```
sbt:dinocpu> testOnly dinocpu.SingleCycleBranchTesterLab2
```

# Part VIII: `jal`

Next, we look at the J-type instructions.
You can think of them as "unconditional branches."

## `jal` instruction details

The following table shows how the `jal` instruction is laid out.

| 31-12                    | 11-7 | 6-0     | Name   |
|--------------------------|------|---------|--------|
| imm[20, 10:1, 11, 19:12] | rd   | 1101111 | jal    |

`jal` stands for "jump and link."
The instruction has the following effect.

```
pc = pc + imm
R[rd] = pc + 4
```

You must properly update any required entities in your code (e.g. the control unit, NextPC unit, etc). Finally, you should wire up any necessary MUXes.

## Testing `jal`

You can run the tests for changes you made for NextPC in this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.NextPCJalTesterLab2
```

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleJALTesterLab2
```

# Part IX: `jalr`

`jalr` is very similar to `jal`, with one difference.
Unlike `jal`, `jalr` has the format of an I-type instruction.

## `jalr` instruction details

The following table shows how the `jalr` instruction is laid out.

| 31-20     | 19-15 | 14-12 | 11-7 | 6-0     | Name   |
|-----------|-------|-------|------|---------|--------|
| imm[11:0] | rs1   | 000   | rd   | 1100111 | jalr   |

`jalr` stands for "jump and link register."
The instruction has the following effect.
(Careful, there's one major difference between this and `jal`!)

```
pc = R[rs1] + imm
R[rd] = pc + 4
```

You must properly update any required entities in your code (e.g. the control unit, NextPC unit, etc). Finally, you should wire up any necessary MUXes.

You can run the tests for changes you made for NextPC in this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.NextPCJalrTesterLab2
```

## Testing `jalr`

You can run the tests for this part with the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleJALRTesterLab2
```

# Testing the entire `NextPC` module

Now, that you have fully implemented the NextPC unit, you can run the tests for the whole unit with the following command:

```
sbt:dinocpu> testOnly dinocpu.NextPCTesterLab2
```

# Part X: Full applications

At this point, you should have a fully implemented RISC-V CPU!
In this final part of the assignment, you will run some full RISC-V applications.

We have provided four applications for you.

* `fibonacci`, which computes the nth Fibonacci number. The initial value of `t1` contains the Fibonacci number to compute, and after computing, the value is found in `t0`.
* `naturalsum`
* `multiplier`
* `divider`

If you have passed all of the above tests, your CPU should execute these applications with no issues!
If you do not pass a test, you may need to dig into the debug output of the test.

**Important Note:** We strongly encourage you to use [single stepper](../documentation/single-stepping.md) to test your design for full applications. It will let you step through the execution one cycle at a time and print information as you go. Details on how to use the single stepper can be found in the [documentation](../documentation/single-stepping.md). You can also watch the video we have provided in the link below to learn how to debug using single stepper. As mentioned earlier, the videos were originally made for spring quarter 2020 (sq20). Just in case you wanted to use any command or text from these videos which contains 'sq20', you just need to convert it to 'sq22' to be applicable to your materials for the current quarter.

[DinoCPU - Debugging your implementation](https://video.ucdavis.edu/playlist/dedicated/0_8bwr1nkj/0_kv1v647d)



## Testing full applications

You can run all of the applications at once with the following test.

```
sbt:dinocpu> testOnly dinocpu.SingleCycleApplicationsTesterLab2
```

To run a single application, you can use the following command:

```
sbt:dinocpu> testOnly dinocpu.SingleCycleApplicationsTesterLab2 -- -z <binary name>
```

# Grading

Grading will be done automatically on Gradescope.
See [the Submission section](#Submission) for more information on how to submit to Gradescope.

| Name                  | Percentage                 |
|-----------------------|----------------------------|
| Each instruction type | 10% each (× 9 parts = 90%) |
| Full programs         | 10%                        |

# Submission

**Warning**: read the submission instructions carefully.
Failure to adhere to the instructions will result in a loss of points.

## Code portion

You will upload the three files that you changed to Gradescope on the [Project 2]()(Will update link when setup) assignment.

- `src/main/scala/components/alucontrol.scala`
- `src/main/scala/components/nextpc.scala`
- `src/main/scala/components/control.scala`
- `src/main/scala/single-cycle/cpu.scala`

Once uploaded, Gradescope will automatically download and run your code.
This should take less than 5 minutes.
For each part of the assignment, you will receive a grade.
If all of your tests are passing locally, they should also pass on Gradescope unless you made changes to the I/O, **which you are not allowed to do**.

Note: There is no partial credit on Gradescope.
Each part is all or nothing.
Either the test passes or it fails.

## Academic misconduct reminder

You are to work on this project **individually**.
You may discuss *high level concepts* with one another (e.g., talking about the diagram), but all work must be completed on your own.

**Remember, DO NOT POST YOUR CODE PUBLICLY ON GITHUB!**
Any code found on GitHub that is not the base template you are given will be reported to SJA.
If you want to sidestep this problem entirely, don't create a public fork and instead create a private repository to store your work.
GitHub now allows everybody to create unlimited private repositories for up to three collaborators, and you shouldn't have *any* collaborators for your code in this class.

# Hints

- Start early! There is a steep learning curve for Chisel, so start early and ask questions on [Piazza](https://piazza.com/class/l17chp6pyg4tw/) and in discussion.
- If you need help, come to office hours, or post your questions on [Piazza](https://piazza.com/class/l17chp6pyg4tw).
- See [common errors](../documentation/common-errors.md) for some common errors and their solutions.

## Single stepper

You can also use the [single stepper](../documentation/single-stepping.md) to step through the execution one cycle at a time and print information as you go.
Details on how to use the single stepper can be found in the [documentation](../documentation/single-stepping.md) and in [DinoCPU - Debugging your implementation](https://video.ucdavis.edu/playlist/dedicated/0_8bwr1nkj/0_kv1v647d).

## `printf` debugging

This is the best style of debugging for this assignment.

- Use `printf` when you want to print *during the simulation*.
  - Note: this will print *at the end of the cycle* so you'll see the values on the wires after the cycle has passed.
  - Use `printf(p"This is my text with a $var\n")` to print Chisel variables. Notice the "p" before the quote!
  - You can also put any Scala statement in the print statement (e.g., `printf(p"Output: ${io.output})`).
  - Use `println` to print during compilation in the Chisel code or during test execution in the test code. This is mostly like Java's `println`.
  - If you want to use Scala variables in the print statement, prepend the statement with an 's'. For example, `println(s"This is my cool variable: $variable")` or `println("Some math: 5 + 5 = ${5+5}")`.
