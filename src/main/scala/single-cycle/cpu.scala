// This file is where all of the CPU components are assembled into the whole CPU

package dinocpu

import chisel3._
import chisel3.util._
import dinocpu.components._

/**
 * The main CPU definition that hooks up all of the other components.
 *
 * For more information, see section 4.4 of Patterson and Hennessy
 * This follows figure 4.21
 */
class SingleCycleCPU(implicit val conf: CPUConfig) extends BaseCPU {
  // All of the structures required
  val pc         = dontTouch(RegInit(0.U))
  val control    = Module(new Control())
  val registers  = Module(new RegisterFile())
  val aluControl = Module(new ALUControl())
  val alu        = Module(new ALU())
  val immGen     = Module(new ImmediateGenerator())
  val nextpc     = Module(new NextPC())
  val (cycleCount, _) = Counter(true.B, 1 << 30)

  control.io    := DontCare
  registers.io  := DontCare
  aluControl.io := DontCare
  alu.io        := DontCare
  immGen.io     := DontCare
  nextpc.io     := DontCare
  io.dmem       := DontCare

  //FETCH
  io.imem.address := pc
  io.imem.valid := true.B

  val instruction = io.imem.instruction
  val opcode = instruction(6,0)
  val rd = instruction(11,7)
  val funct3 = instruction(14,12)
  val rs1 = instruction(19,15)
  val rs2 = instruction(24,20)
  val funct7 = instruction(31,25)

  control.io.opcode := opcode

  registers.io.readreg1 := rs1
  registers.io.readreg2 := rs2
  registers.io.writereg := rd
  registers.io.wen := (registers.io.writereg =/= 0.U)
  registers.io.writedata := alu.io.result

  aluControl.io.itype := control.io.itype
  aluControl.io.aluop := control.io.aluop
  aluControl.io.funct3 := funct3
  aluControl.io.funct7 := funct7

  alu.io.operation := aluControl.io.operation
  alu.io.inputx := registers.io.readdata1
  alu.io.inputy := registers.io.readdata2

  nextpc.io.pc_or_x := pc
  pc := nextpc.io.nextpc
}

/*
 * Object to make it easier to print information about the CPU
 */
object SingleCycleCPUInfo {
  def getModules(): List[String] = {
    List(
      "dmem",
      "imem",
      "control",
      "registers",
      "csr",
      "aluControl",
      "alu",
      "immGen",
      "nextpc"
    )
  }
}
