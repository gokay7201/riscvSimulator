import java.util.function.ToDoubleBiFunction;

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

        //RS1 RS2 LERI REGLERDEN CEKTIM AMA ILERDE HAZARD DETECTION YAPTIGIMIZDA FORWARDED GELEN DATA FALAN OLURSA IF KOSULLARIYLA DUZENLERI0Z

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
            cpu.ID_EX_rs1_id = rs1; // id of rs1
            cpu.ID_EX_rs2_id = rs2; //id of rs2            
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
            cpu.ID_EX_rs1_id = rs1; // id of rs1
            cpu.ID_EX_rs2_id = -1; // BE CAREFUL JUST FOR SIMPLICITY IN CONTROL -1 IF EMPTY 
            cpu.ID_EX_rs1=cpu.registers[rs1];  
            cpu.ID_EX_rs2=rs2;    //no need to decode already immediate
            cpu.ID_EX_rd=rd;

            return;

        }

        else{   //opeation is sd

            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1)); 
            int rs2=Integer.parseInt(cpu.IF_ID_rs2);   // this is already immidiate
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));

            cpu.ID_EX_rs1_id = rs1; // id of rs1
            cpu.ID_EX_rs2_id = -1;  // maybe using in forwarding the store value rd also as rs id  ATTENTION RISKY CODE LINE
            cpu.ID_EX_rs1=cpu.registers[rs1];  
            cpu.ID_EX_rs2=rs2;    //no need to decode
            cpu.ID_EX_rd=rd;   //decode also rd since it will be stored, we fetched its value
            cpu.ID_EX_storeData = cpu.registers[rd];
            return;

        }


        

    }
    public void executionALU(RiscvCpu cpu){

        String type=cpu.ID_EX_type; 
        cpu.EX_MEM_type=type;
        if(type.equals("nop")){
            return;
        }

        int op1= cpu.ID_EX_rs1;
        int op2 = cpu.ID_EX_rs2;
        int rd= cpu.ID_EX_rd;

        // aslinda forwarding unit yapmak icin bize registerlarin icindeki degerlerin yanında register kimlikleri de lazım
        // FORWARDING CONTROLLERI BURDA YAPILACAK ISLEME BASLAMADAN ONCE DOGRU VERILERI ALICAZ VE ONA GORE ISLEME TABII TUTACAGIZ
       
        // TODO
        int id_op1 = cpu.ID_EX_rs1_id;
        int id_op2 = cpu.ID_EX_rs2_id;
        

        // we can place the mem hazard forwarding right here somewhere 

        String exmem_type = cpu.EX_MEM_type;
        int exmem_rd = cpu.EX_MEM_rd;
        
        // if condition = if ( EX_MEM.REGWRITE)
        if(exmem_type.equals("add")||exmem_type.equals("sub")||exmem_type.equals("and")||exmem_type.equals("or")||exmem_type.equals("addi")||exmem_type.equals("ld")){
            if(exmem_rd != 0){
                if(id_op1 == exmem_rd){
                    op1 = cpu.EX_MEM_aluResult;
                }else if(id_op2 == exmem_rd){
                    // id_op2 is -1 for addi ld and sd
                    op2 = cpu.EX_MEM_aluResult;
                } // store datasi icin forward yazmadik
            }
        }





        if(type.equals("sd")){
            cpu.EX_MEM_aluResult = op1 + op2;
            cpu.EX_MEM_storeData = cpu.ID_EX_storeData; // aslinda bu store datasini fowardinge gore guncellemek lazım 
            cpu.EX_MEM_rd = rd;
            return;
        }


        switch(type){
            case "add":
                cpu.EX_MEM_aluResult = op1 + op2;
                break;
            case "sub":
                cpu.EX_MEM_aluResult = op1 - op2;
                 break;
            case "and":
                cpu.EX_MEM_aluResult = op1 & op2;
                break;
            case "or":
                cpu.EX_MEM_aluResult = op1 | op2;
                break;
            case "addi":
                cpu.EX_MEM_aluResult = op1 + op2;
                break;
            case "ld":
                cpu.EX_MEM_aluResult = op1 + op2;
                break;
        }

        cpu.EX_MEM_rd= rd;  
        
        // it will also be better to send identity of rs registers ?
        

    }
    public void memoryOperations(RiscvCpu cpu){
        String type=cpu.EX_MEM_type; 
        cpu.MEM_WB_type=type;
        if(type.equals("nop")){
            return;
        }
        int rd= cpu.EX_MEM_rd;
        int aluResult = cpu.EX_MEM_aluResult;

        if(type.equals("add")||type.equals("sub")||type.equals("and")||type.equals("or")||type.equals("addi")){
            cpu.MEM_WB_rd = rd;
            cpu.MEM_WB_wbData = aluResult;
            return;

        }
        if(type.equals("ld")){
            cpu.MEM_WB_rd = rd;
            cpu.MEM_WB_wbData = cpu.DataMemory[aluResult];
            return;
        }
        if(type.equals("sd")){
            cpu.DataMemory[aluResult] = cpu.EX_MEM_storeData;
            cpu.MEM_WB_type = "nop";
            // should i increase the stall number ???
            return;
        }


    }
    public void writebackOperations(RiscvCpu cpu){
        String type=cpu.MEM_WB_type;

        if(type.equals("nop")){
            return;
        }
        int rd= cpu.MEM_WB_rd;
        int writebackData = cpu.MEM_WB_wbData;

        if(rd != 0)
            cpu.registers[rd] = writebackData;
        


    }



}
