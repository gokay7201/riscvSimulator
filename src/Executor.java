public class Executor {
    

    /**
     * Takes program counter and fetches the following instruction from instruction memory.
     * Passes the operands and type to ID_ID registers
     */
    public void instructionFetch(RiscvCpu cpu){

        if(cpu.IF_ID_flush){  //flush the new instruction
            cpu.IF_ID_type="nop";
            return;
        }

        cpu.IF_ID_type=cpu.InstructionMemory[cpu.pc];
        cpu.IF_ID_rd=cpu.InstructionMemory[cpu.pc+1];
        cpu.IF_ID_rs1=cpu.InstructionMemory[cpu.pc+2];
        cpu.IF_ID_rs2=cpu.InstructionMemory[cpu.pc+3];
        
    }

    public void instructionDecode(RiscvCpu cpu){  

        //RS1 RS2 LERİ REGLERDEN ÇEKTİM AMA İLERDE HAZARD DETECTION YAPTIĞIMIZDA FORWARDED GELEN DATA FALAN OLURSA IF KOŞULLARIYLA DÜZENLERİZ

        String type=cpu.IF_ID_type;   
        cpu.ID_EX_type=type;  //move type to ID/EX
        cpu.pc+=4;  //pc is updated (if there is branch, it will be updated again)
        cpu.IF_ID_flush=false;   //no flush signal yet

        if(type.equals("nop")){
            return;
        }

        if(type.equals("beq")){   //check branch

            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1)); //get register numbers
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));  

            if(cpu.registers[rs1]==cpu.registers[rd]){   //branch condition
                int newPc=cpu.labels.get(cpu.IF_ID_rs2)-4;   //1 instruction will be flushed, so pc+=4 will start from correct place
                cpu.pc=newPc;    //since we update pc here,new instruction will be fetched correctly
                cpu.IF_ID_flush=true;    //signal flush to instruction fetch
                cpu.stall++;   
            }
            cpu.ID_EX_type="nop";  //beq instruction do not have to be stored anymore for following stages
            return;
        }

        

        if(type.equals("add")||type.equals("sub")||type.equals("and")||type.equals("or")){
            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1));  //rs1 without x
            int rs2=Integer.parseInt(cpu.IF_ID_rs2.substring(cpu.IF_ID_rs2.indexOf("x")+1));  //rs2 without x
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));  //rd without x

            if(rd==0){
                cpu.ID_EX_type="nop";   //x0 can not be changed!
                return;
            }

            cpu.ID_EX_rs1=cpu.registers[rs1];  //get the register values of corresponding operands and put it to ID/EX
            cpu.ID_EX_rs2=cpu.registers[rs2];
            cpu.ID_EX_rd=rd;  //get the register enumeration only(we do not care the value of destination register)
            return;
        }

        if(type.equals("addi")||type.equals("ld")){
            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1)); 
            int rs2=Integer.parseInt(cpu.IF_ID_rs2);   // this is already immidiate
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));

            if(rd==0){
                cpu.ID_EX_type="nop";   //x0 can not be changed!
                return;
            }
            cpu.ID_EX_rs1=cpu.registers[rs1];  
            cpu.ID_EX_rs2=rs2;    //no need to decode already immediate
            cpu.ID_EX_rd=rd;

            return;

        }

        else{   //opeation is sd

            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1)); 
            int rs2=Integer.parseInt(cpu.IF_ID_rs2);   // this is already immidiate
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));

            cpu.ID_EX_rs1=cpu.registers[rs1];  
            cpu.ID_EX_rs2=rs2;    //no need to decode
            cpu.ID_EX_rd=cpu.registers[rd];   //decode also rd since it will be stored, we fetched its value
            return;

        }


        

    }



}
