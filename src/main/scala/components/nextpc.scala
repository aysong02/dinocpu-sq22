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
 * Input: ltf            true if inputx < input y in ALU signed
 * Input: ltuf           true if inputx < input y in ALU unsigned
 * Input: funct3         the funct3 from the instruction
 * Input: pc_or_x        the *current* program counter for this instruction or register input
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
    val ltuf    = Input(Bool())
    val funct3  = Input(UInt(3.W))
    val pc_or_x = Input(UInt(32.W))
    val imm     = Input(UInt(32.W))

    val nextpc  = Output(UInt(32.W))
    val taken   = Output(Bool())
  })

  // Your code goes here
  when (io.branch) {
    when (io.funct3 === "b000".U)      { io.taken := io.eqf } // beq
    .elsewhen (io.funct3 === "b001".U) { io.taken := !io.eqf } // bne
    .elsewhen (io.funct3 === "b100".U) { io.taken := io.ltf } // blt
    .elsewhen (io.funct3 === "b101".U) { io.taken := !io.ltf } // bge
    .elsewhen (io.funct3 === "b110".U) { io.taken := io.ltuf } // bltu
    .elsewhen (io.funct3 === "b111".U) { io.taken := !io.ltuf } // bgeu
    .otherwise                         { io.taken := false.B } // invalid

    when (io.taken) {
      io.nextpc := io.pc_or_x + io.imm
    } .otherwise {
      io.nextpc := io.pc_or_x + 4.U
    }
  } .elsewhen (io.jal || io.jalr) {
    io.taken := true.B // All jumps are taken
    io.nextpc := io.pc_or_x + io.imm
  } .otherwise {
    io.nextpc := io.pc_or_x + 4.U
    io.taken  := false.B
  }

}
