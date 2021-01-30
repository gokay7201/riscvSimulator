import java.util.function.ToDoubleBiFunction;

public class Executor {
    

    public RiscvCpu cpu;

    public Executor(RiscvCpu cpu){
        this.cpu= cpu;
    }


    /**
     * Takes program counter and fetches the following instruction from instruction memory.
     * Passes the operands and type to IF_ID registers
     * Puts nop if flush signal is received
     * Puts nop if there is no more instruction to fetch
     * Counts executed instructions for cpi calculation
     */
    public void instructionFetch(){
        
        if(cpu.IF_ID_flush){  //flush the new instruction
            cpu.IF_ID_type="nop";
            return;
        }

        if(cpu.InstructionMemory[cpu.pc]==null){  //instructions are over but need to wait pipeline to complete
            cpu.IF_ID_type="nop";
            return;
        }
        if(cpu.pc != cpu.old_pc)   //if last pc is equal to current pc, stall occured
            cpu.executedIns++;   //do not count stalled instruction twice

        //filling IF/ID
        cpu.IF_ID_type=cpu.InstructionMemory[cpu.pc];
        cpu.IF_ID_rd=cpu.InstructionMemory[cpu.pc+1];
        cpu.IF_ID_rs1=cpu.InstructionMemory[cpu.pc+2];
        cpu.IF_ID_rs2=cpu.InstructionMemory[cpu.pc+3];

        cpu.old_pc = cpu.pc;
    }



    /**
     * Reads and decodes the instruction passed from InstructionFetch
     * (Be careful, in main loop, this function is called "before" instruction fetch, thus decoded instruction is the
     * instruction fetched in last cycle) This is how we maintain parallelism in the pipeline
     * Decoding procedure varies between instructions
     */
    public void instructionDecode(){  

        //In load use hazards, ALU signals stall, so ALU needs to fetch the same instruction in next cycle
        //Thus, content of ID/EX registers should not change. In case of a stall, this function does nothing.
        if(cpu.IF_ID_putStall){   
            cpu.IF_ID_putStall=false;
            return;
        }

        //This part is the general procedure for ID
        //The type of instruction is moved from IF/ID to ID/EX, for next cycles
        //PC=PC+4
        String type=cpu.IF_ID_type;   
        cpu.ID_EX_type=type;  //move type to ID/EX
        cpu.pc+=4;  //pc is updated (if there is branch, it will be updated again)


        cpu.IF_ID_flush=false;   //Flush variable will be used in some cases


        //If type is nop, there is nothing to do
        //Nop can be received when IF flushes 
        if(type.equals("nop")){
            return;
        }
        

        //Branching decision is made in ID
        //2 registers to compare are decoded, their values are compared
        //But to obtain the values, forwarding or stall may be necessary, hazard detection is conducted 
        if(type.equals("beq")){   //check branch

            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1)); //get register numbers
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));  

            long op1=0; long op2=0;
            boolean handle=false;

            //HAZARD DETECTION
            if(!cpu.EX_MEM_type.equals("sd") && !cpu.EX_MEM_type.equals("nop")){   //if previous instruction is sd or nop, no dependency
               
                if(cpu.EX_MEM_rd==rs1 || cpu.EX_MEM_rd==rd){  //add beq  /  ld beq  sequences with dependent registers
                    
                    //Stall procedure
                    cpu.ID_EX_type="nop";
                    cpu.pc-=4;
                    cpu.stall++;
                    return;
                   
                }
                
                handle=true;
            }

            if(!cpu.MEM_WB_type.equals("sd") && !cpu.MEM_WB_type.equals("nop")){   
                
                if(cpu.MEM_WB_rd==rs1 || cpu.MEM_WB_rd==rd){  //add nop beq  /  ld nop beq sequences
                    if(cpu.MEM_WB_type.equals("ld")){   //stall one more for ld
                        cpu.ID_EX_type="nop";
                        cpu.pc-=4;
                        cpu.stall++;
                        return;
                    }
                    //forwarding is sufficient for others
                    op1=(cpu.MEM_WB_rd==rs1)?cpu.MEM_WB_wbData:cpu.registers[rs1];   //add data is ready
                    op2=(cpu.MEM_WB_rd==rd)?cpu.MEM_WB_wbData:cpu.registers[rd];   //add data is ready
                }
                
                handle=true;
            }

            
            //If there is no hazard, just fetch the register content
            if(!handle){
                op1=cpu.registers[rs1];
                op2=cpu.registers[rd];
            }



            //After fetching data, check for breanch condition 
            if(op1==op2){   
                int newPc=cpu.labels.get(cpu.IF_ID_rs2)-4;   //Move the pc to jump address
                cpu.pc=newPc;    //since we update pc here,new instruction will be fetched correctly
                cpu.IF_ID_flush=true;    //signal flush to instruction fetch
                cpu.stall++;   
            }
            cpu.ID_EX_type="nop";  //beq instructioncan be considered as nop for following stages
            return;
        }

       
        
        if(type.equals("add")||type.equals("sub")||type.equals("and")||type.equals("or")){

            //Get enumeration of registers
            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1));  //rs1 without x
            int rs2=Integer.parseInt(cpu.IF_ID_rs2.substring(cpu.IF_ID_rs2.indexOf("x")+1));  //rs2 without x
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));  //rd without x

            if(rd==0){
                cpu.ID_EX_type="nop";   //x0 can not be changed!
                return;
            }

            //store enumeration in ID/EX
            cpu.ID_EX_rs1_id = rs1; // id of rs1
            cpu.ID_EX_rs2_id = rs2; //id of rs2   
            
            //Decode the register values and keep them also in ID/EX
            //Storing in intermediate registers will help us to use them in next stages
            cpu.ID_EX_rs1=cpu.registers[rs1];  //get the register values of corresponding operands and put it to ID/EX
            cpu.ID_EX_rs2=cpu.registers[rs2];

            cpu.ID_EX_rd=rd;  //get the register enumeration only(we do not care the value of destination register)
            return;
        }


        //We are decoding 2 regs and 1 immidiate instead of 3 regs
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

            //Sd data hazards should be handled here since the data to store can
            //be fetched from register or obtained by forwarding

            int rs1=Integer.parseInt(cpu.IF_ID_rs1.substring(cpu.IF_ID_rs1.indexOf("x")+1)); 
            int rs2=Integer.parseInt(cpu.IF_ID_rs2);   // this is already immidiate
            int rd=Integer.parseInt(cpu.IF_ID_rd.substring(cpu.IF_ID_rd.indexOf("x")+1));

            

            cpu.ID_EX_rs1_id = rs1; // id of rs1
            cpu.ID_EX_rs2_id = -1;  // maybe using in forwarding the store value rd also as rs id 
            cpu.ID_EX_rs1=cpu.registers[rs1];  
            cpu.ID_EX_rs2=rs2;    //no need to decode
            cpu.ID_EX_rd=rd;   //id of source reg for sd

            //Checking data hazard (if two instructions have same destination registers and second instruction is sd)
            if(rd==cpu.EX_MEM_rd && !cpu.EX_MEM_type.equals("nop") && !cpu.EX_MEM_type.equals("sd")){   //add/sd   or   ld/sd sequences
                if(cpu.EX_MEM_type.equals("ld")){  //ld sd requires stall
                    cpu.ID_EX_type="nop";
                    cpu.pc-=4;
                    cpu.stall++;
                }
                else{
                    cpu.ID_EX_storeData=cpu.EX_MEM_aluResult;  //forward the calculated alu result to storedata
                }
            }
            else if(rd==cpu.MEM_WB_rd && !cpu.MEM_WB_type.equals("nop") && !cpu.MEM_WB_type.equals("sd")){ //add/nop/sd   or    ld/nop/sd  sequneces
                cpu.ID_EX_storeData=cpu.MEM_WB_wbData;   //forward from memwb
            }
            else{
            cpu.ID_EX_storeData = cpu.registers[rd];   //value of source reg for sd
            }
            return;

        }

    }

    /**
     * Checks data hazards and execute arithemtic operations after receiving operands
     */
    public void executionALU(){

        //Type is moved to next intermediate register
        String type=cpu.ID_EX_type; 
        cpu.EX_MEM_type=type;    

        //Do nothing
        if(type.equals("nop")){
            return;
        }

        long op1= cpu.ID_EX_rs1;
        long op2 = cpu.ID_EX_rs2;
        int rd= cpu.ID_EX_rd;

    
        int id_op1 = cpu.ID_EX_rs1_id;
        int id_op2 = cpu.ID_EX_rs2_id;
        

        //Get the types and content of previous intructions(which are ahead in pipeline) to do hazard checks
        String exmem_type = cpu.EX_MEM_type;
        int exmem_rd = cpu.EX_MEM_rd;

        String memwb_type = cpu.MEM_WB_type;
        String wbend_type = cpu.WB_END_type;
        int wbend_rd = cpu.WB_END_rd;

        
        //HAZARD DETECTION
            if(exmem_rd != 0){  //Check destination is not x0

                boolean op1_forwarded=false;
                boolean op2_forwarded=false;

                if(id_op1 == exmem_rd||id_op2==exmem_rd){   //add/add   or   ld/add squences

                    if(id_op1 == exmem_rd){   
                        if(memwb_type.equals("ld")){    //stall (make add nop ld in the pipeline) if previous instruction is ld, stall needed
                            cpu.IF_ID_putStall=true;   //ID should keep its recent instruction so signal the stall
                            cpu.EX_MEM_type="nop";   //put nop between
                            cpu.stall++;
                        }
                        else if(memwb_type.equals("sd")||memwb_type.equals("nop")){ // sd add  
                            //no problem no dependency
                        }
                        //add add hazard  or add ld hazard or add sd hazard
                        else{  //just forwarding is enough
                            op1_forwarded=true;
                            op1 = cpu.EX_MEM_aluResult;
                        }
                    }

                    //Same for second operand
                    if(id_op2 == exmem_rd){    //not else if becasue 2 forwarding may be necessary(op1=op2)

                        if(memwb_type.equals("ld")){  //signal stall to id and go on with ld
                            cpu.IF_ID_putStall=true;
                            cpu.EX_MEM_type="nop";
                            if(id_op1!=id_op2){
                                cpu.stall++;
                            }
                        }
                        else if(memwb_type.equals("sd")||memwb_type.equals("nop")){
                            //no problem
                        }
                        else{  //just forward   
                            if(!exmem_type.equals("ld") && !exmem_type.equals("sd")){   //forwarding to second operand is wrong since immidiate 
                                op2_forwarded=true;
                                op2 = cpu.EX_MEM_aluResult;
                            }
                        }
                    }
                }

                
                //Hazard checks with one more ahead instruction
                if(!wbend_type.equals("sd") && !wbend_type.equals("nop")){ 
                    //ld/nop/add  or  add/nop/add  or    ld/nop/ld sequences
                    //Consider nop as any independent operation. (We look for dependency between 2 instructions which have another instruction between each other)

                    if(id_op1 == wbend_rd && !op1_forwarded){   //if op1 is forwarded with the condition above, it is prior, do not forward again
                        op1=cpu.WB_END_wbData;
                    }
                    if(id_op2 == wbend_rd && !op2_forwarded && !exmem_type.equals("ld") && !exmem_type.equals("sd")){  
                        //if exmem type=ld, op2 is immidiate, do not change that anyway
                        op2=cpu.WB_END_wbData;
                    }
        
                }

                
                
            }
        


        if(type.equals("sd")){
            cpu.EX_MEM_aluResult = op1 + op2;
            cpu.EX_MEM_storeData = cpu.ID_EX_storeData;
            cpu.EX_MEM_rd = rd;
            return;
        }


        //ALU operations
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

    }



    /**
     * For ld and sd ops, memory operations are conducted
     */
    public void memoryOperations(){
        String type=cpu.EX_MEM_type; 
        cpu.MEM_WB_type=type;
        if(type.equals("nop")){
            return;
        }
        int rd= cpu.EX_MEM_rd;
        long aluResult = cpu.EX_MEM_aluResult;

        if(type.equals("add")||type.equals("sub")||type.equals("and")||type.equals("or")||type.equals("addi")){
            cpu.MEM_WB_rd = rd;
            cpu.MEM_WB_wbData = aluResult;
            return;

        }
        if(type.equals("ld")){
            cpu.MEM_WB_rd = rd;
            cpu.MEM_WB_wbData = cpu.DataMemory[(int)aluResult];
            return;
        }
        if(type.equals("sd")){
            cpu.DataMemory[(int)aluResult] = cpu.EX_MEM_storeData;
            cpu.MEM_WB_type = "nop";   //job for sd ended, convert it to nop
            return;
        }
    }

    //The typical write back stage
    public void writebackOperations(){
        String type=cpu.MEM_WB_type;
        cpu.WB_END_type=type;

        if(type.equals("nop")){
            return;
        }
        int rd= cpu.MEM_WB_rd;
        long writebackData = cpu.MEM_WB_wbData;
        cpu.WB_END_wbData=writebackData;
        cpu.WB_END_rd=rd;

        if(rd != 0){
            cpu.registers[rd] = writebackData;
        }


    }



}
