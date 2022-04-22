// This file contains ALU control logic.

package dinocpu.components

import chisel3._
import chisel3.util._

/**
 * The ALU control unit
 *
 * Input:  aluop,  0 for ld/st, 1 for R-type
 * Input:  funct7, the most significant bits of the instruction
 * Input:  funct3, the middle three bits of the instruction (12-14)
 * Output: operation, What we want the ALU to do.
 *
 * For more information, see Section 4.4 and A.5 of Patterson and Hennessy.
 * This is loosely based on figure 4.12
 */
class ALUControl extends Module {
  val io = IO(new Bundle {
    val aluop     = Input(Bool())
    val itype     = Input(Bool())
    val funct7    = Input(UInt(7.W))
    val funct3    = Input(UInt(3.W))

    val operation = Output(UInt(4.W))
  })

  io.operation := "b1111".U

  // Your code goes here
  switch (io.funct3) {
    is ("b000".U) { io.operation := Mux( !io.funct7(5), "b0110".U, "b0100".U) }
    is ("b001".U) { io.operation := "b1001".U }
    is ("b010".U) { io.operation := "b1000".U }
    is ("b011".U) { io.operation := "b0001".U }
    is ("b100".U) { io.operation := "b0000".U }
    is ("b101".U) { io.operation := Mux(io.funct7(5), "b0011".U, "b0010".U) }
    is ("b110".U) { io.operation := "b0101".U }
    is ("b111".U) { io.operation := "b0111".U }
  }
}
