package exp;

import java.io.IOException;
import java.util.BitSet;

public class GramAnaly {
	private Symbol cursym; 
	WordAnaly word;
	public SymbolTable table; 
	public Interpreter interp; 
	private int dx = 0;
	private BitSet declbegsys;
	private BitSet statbegsys;
	private BitSet facbegsys;
	public GramAnaly(String p) {
		// TODO Auto-generated constructor stub
		    word=new WordAnaly(p);
		    table=new SymbolTable();
		    interp= new Interpreter();
		    declbegsys = new BitSet(Symbol.symnum);
	        declbegsys.set(Symbol.constsym);                 //将指定索引处的位设置为 true。
	        declbegsys.set(Symbol.varsym);
	        declbegsys.set(Symbol.procsym);

	        statbegsys = new BitSet(Symbol.symnum);
	        statbegsys.set(Symbol.beginsym);
	        statbegsys.set(Symbol.callsym);
	        statbegsys.set(Symbol.ifsym);
	        statbegsys.set(Symbol.whilesym);
	        statbegsys.set(Symbol.repeatsym);

	        facbegsys = new BitSet(Symbol.symnum);
	        facbegsys.set(Symbol.ident);
	        facbegsys.set(Symbol.number);
	        facbegsys.set(Symbol.lparen);
		
	}
	
	 public void getsym() {
		 
	        cursym = word.getsym();
	  }
	 
	 public void parse() {
	  
	        getsym();
	        
	        block(0);                                                       //解析<分程序>

	 }
	 
	 public void block(int lev) {
	       

	        int dx0 = dx,               //记录本层之前的数据量,以便返回时恢复
	            tx0 = table.tablePtr,   //记录本层名字的初始位置
	            cx0;
	        //置初始值为3的原因是：
	        //每一层最开始的位置有三个空间用于存放静态链SL、动态链DL和返回地址RA
	        dx = 3;
	        //当前pcode代码的地址，传给当前符号表的addr项
	        table.get(table.tablePtr).addr = interp.arrayPtr;                      //在符号表的当前位置记录下这个jmp指令在代码段中的位置 
	        interp.gen(Pcode.JMP, 0, 0);                             //JMP 0 0

	     
	        //分析<说明部分>
	        do {
	            //<常量说明部分> ::= const<常量定义>{,<常量定义>};
	            if (cursym.symtype == Symbol.constsym) {                 //例如const a=0,b=0,... ...,z=0;
	                getsym();
	                constdeclaration(lev);                            //<常量定义>
	                while (cursym.symtype == Symbol.comma) {
	                    getsym();
	                    constdeclaration(lev);
	                }

	                if (cursym.symtype == Symbol.semicolon)   //如果是分号，表示常量申明结束
	                {
	                    getsym();
	                } 
	            }

	            //<变量说明部分>
	            //var<标识符>{,<标识符>};
	            if (cursym.symtype == Symbol.varsym) {                       //读入的数为var
	                getsym();
	                vardeclaration(lev);                                  //识别<标识符>
	                while (cursym.symtype == Symbol.comma) {              //识别{,<标识符>}
	                    getsym();
	                    vardeclaration(lev);
	                }
	                if (cursym.symtype == Symbol.semicolon) //如果是分号，表示变量申明结束
	                {
	                    getsym();
	                } 
	            }

	
	       while (cursym.symtype == Symbol.procsym) {                 //如果是procedure
	                getsym();
	                if (cursym.symtype == Symbol.ident) {                      //填写符号表
	                    table.enter(cursym, SymbolTable.Item.procedure, lev, dx);                                             //当前作用域的大小 
	                    getsym();
	                } 
	                if (cursym.symtype == Symbol.semicolon)               //分号，表示<过程首部>结束
	                {
	                    getsym();
	                } 
	                
	               
	                block(lev + 1);                                  //嵌套层次+1，分析分程序

	               
	            }

	   
	        } while (declbegsys.get(cursym.symtype));                     //直到没有声明符号

	        //开始生成当前过程代码
	
	        SymbolTable.Item item = table.get(tx0);
	        interp.pcodeArray[item.addr].a = interp.arrayPtr;//过程入口地址填写在pcodeArray中的jmp 的第二个参数
	        item.addr = interp.arrayPtr;       //当前过程代码地址
	        item.size = dx;//dx:一个procedure中的变量数目+3 ，声明部分中每增加一条声明都会给dx+1
	        //声明部分已经结束，dx就是当前过程的堆栈帧大小
	
	        cx0 = interp.arrayPtr;
	        //生成分配内存代码，
	        interp.gen(Pcode.INT, 0, dx);                               

	      
	        statement(lev);
	        
	 
	        interp.gen(Pcode.OPR, 0, 0);                                               //每个过程出口都要使用的释放数据段指令

	        
	        table.debugTable(0);

	        dx = dx0;                                                                           //恢复堆栈帧计数器
	        table.tablePtr = tx0;                                                                     //回复名字表位置
	    }
	 

  void constdeclaration(int lev) {
    if (cursym.symtype == Symbol.ident) {                                                 //识别符
        String id = cursym.id;//先保存起来
        getsym();
        if (cursym.symtype == Symbol.eql || cursym.symtype == Symbol.becomes) {     //等于或者赋值符号
            if (cursym.symtype == Symbol.becomes) {
                System.out.println("定义不能用赋值号");                                                         //把=写成了：=
            }
            getsym();																	  //自动进行了错误纠正使编译继续进行，把赋值号当作等号处理
            if (cursym.symtype == Symbol.number) {
                cursym.id = id;
                table.enter(cursym, SymbolTable.Item.constant, lev, dx);           //将常量填入符号表
                getsym();
            } else {
            	System.out.println("定义常量等号后面出错");                                                              //常量说明=后应是数字
            }
        } else {
        	System.out.println("定义符号不对");                                                            //常量说明标志后应是=
        }
     } else {
    	 System.out.println("请对标识符进行定义");                                                                //const后应是标识符
     }
   }
   void vardeclaration(int lev) {
      if (cursym.symtype == Symbol.ident) {
          /**
           * 填写名字表并改变堆栈帧计数器 符号表中记录下标识符的名字、它所在的层及它在所在层中的偏移地址
           */
           table.enter(cursym, SymbolTable.Item.variable, lev, dx);
          /**
           * 变量定义过程中,会用dx变量记录下局部数据段分配的空间个数
           */
           dx++;
           getsym();
       } else {
           //myErr.report(4,lex.lineCnt);                                                   //var后应是标识符
       }
   }
   void statement(int lev) {
       // FIRST(statement)={ident,read,write,call,if, while}
       switch (cursym.symtype) {
           case Symbol.ident:
               praseAssignStatement(lev);
               break;
           case Symbol.readsym:
               praseReadStatement(lev);
               break;
           case Symbol.writesym:
               praseWriteStatement(lev);
               break;
           case Symbol.callsym:
               praseCallStatement(lev);
               break;
           case Symbol.ifsym:
              // praseIfStatement(lev);
               break;
           case Symbol.beginsym:
               praseBeginStatement(lev);
               break;
           case Symbol.whilesym:
               praseWhileStatement(lev);
               break;
           case Symbol.repeatsym:
              // praseRepeatStatement(lev);
               break;
           default:
               //err
               break;
       }
    }
    private void praseBeginStatement(int lev) {
       getsym();
      
       statement(lev);
       //循环分析{;<语句>},直到下一个符号不是语句开始符号或者收到end
       while (statbegsys.get(cursym.symtype) || cursym.symtype == Symbol.semicolon) {
           if (cursym.symtype == Symbol.semicolon) {
               getsym();
           } else {
              // myErr.report(10,lex.lineCnt);                                                //缺少分号
           }
           statement(lev);
       }
       if (cursym.symtype == Symbol.endsym) //若为end ，statement解析成功
       {
           getsym();
       } else {
           //myErr.report(17,lex.lineCnt);                                                  //缺少end 或者分号
       }
    }
    private void praseAssignStatement(int lev) {
        //从符号表中找到该标识符的信息
        int index = table.position(cursym.id);
        if (index > 0) {
            SymbolTable.Item item = table.get(index);
            if (item.type == SymbolTable.Item.variable) {                            //标识符
                getsym();
                if (cursym.symtype == Symbol.becomes) {
                    getsym();
                } else {
                    //myErr.report(13,lex.lineCnt);                                                //没有检测到赋值符号
                }
               
                expression(lev);                                         //解析表达式
                //expression将执行一系列指令，
                //但最终结果将会保存在栈顶，
                //执行sto命令完成赋值
                interp.gen(Pcode.STO, lev - item.lev, item.addr);
            } else {
                //myErr.report(12,lex.lineCnt);                                                    //不可向常量或过程名赋值		
            }
        } else {
            //myErr.report(11,lex.lineCnt);                                                         //标识符未说明
        }
    }
    
    private void expression(int lev) {
        if (cursym.symtype == Symbol.plus || cursym.symtype == Symbol.minus) {                                 //分析[+|-]<项>
            int addOperatorType = cursym.symtype;
            getsym();
            
            term(lev);
            if (addOperatorType == Symbol.minus) //OPR 0 1:：NEG取反
            {
                interp.gen(Pcode.OPR, 0, 1);
            }
            // 如果不是负号就是正号，不需生成相应的指令
        } else {
      
            term(lev);
        }

        //分析{<加法运算符><项>}
        while (cursym.symtype == Symbol.plus || cursym.symtype == Symbol.minus) {
            int addOperatorType = cursym.symtype;
            getsym();
           
            term(lev);
            
            interp.gen(Pcode.OPR, 0, addOperatorType);                                    //opr 0 2:执行加法,opr 0 3:执行减法
        }
    }

    /**
     * 分析<项>
     * <项> ::= <因子>{<乘法运算符><因子>}
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void term(int lev) {
        //分析<因子>
       
        factor(lev);                                                                               //先分析<因子>         

        //分析{<乘法运算符><因子>}
        while (cursym.symtype == Symbol.mul || cursym.symtype == Symbol.div) {
            int mulOperatorType = cursym.symtype;                                                          //4表示乘法 ,5表示除法       
            getsym();
            factor(lev);
            interp.gen(Pcode.OPR, 0, mulOperatorType);                                        //乘法:OPR 0 4 ,除法:OPR 0 5
        }
    }

    /**
     * 分析<因子>
     * <因子>=<标识符>|<无符号整数>|'('<表达式>')' 开始因子处理前，先检查当前token是否在facbegsys集合中。
     * 如果不是合法的token，抛24号错误，并通过fsys集恢复使语法处理可以继续进行
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void factor(int lev) {
       

        if (facbegsys.get(cursym.symtype)) {
            if (cursym.symtype == Symbol.ident) {                            //因子为常量或变量或者过程名
                int index = table.position(cursym.id);
                if (index > 0) {                                               //大于0:找到，等于0:未找到
                    SymbolTable.Item item = table.get(index);
                    switch (item.type) {
                        //如果这个标识符对应的是常量，值为val，生成lit指令，把val放到栈顶
                        case SymbolTable.Item.constant:                        //名字为常量
                            interp.gen(Pcode.LIT, 0, item.value);           //生成lit指令，把这个数值字面常量放到栈顶
                            break;
                        case SymbolTable.Item.variable:                         //名字为常量
                            //把位于距离当前层level的层的偏移地址为adr的变量放到栈顶
                            interp.gen(Pcode.LOD, lev - item.lev, item.addr);
                            break;
                        case SymbolTable.Item.procedure:                     //常量
                            //myErr.report(21,lex.lineCnt);                                   //表达式内不可有过程标识符
                            break;
                    }
                } else {
                   // myErr.report(11,lex.lineCnt);                                      //标识符未声明
                }
                getsym();
            } else if (cursym.symtype == Symbol.number) {               //因子为数
                int num = cursym.num;
                if (num > SymbolTable.addrMax) {                                   //数越界
                    //myErr.report(31,lex.lineCnt);
                    //num = 0;
                }
                interp.gen(Pcode.LIT, 0, num);                     //生成lit指令，把这个数值字面常量放到栈顶
                getsym();
            } else if (cursym.symtype == Symbol.lparen) {                 //因子为表达式：'('<表达式>')'
                getsym();
              
                expression(lev);
                if (cursym.symtype == Symbol.rparen) //匹配成功
                {
                    getsym();
                } else {
                    //myErr.report(22,lex.lineCnt);                                   //缺少右括号
                }
            } else //做补救措施
            {
                //test(fsys, facbegsys, 23);                         //一个因子处理完毕，遇到的token应在fsys集合中
            }																			 //如果不是，抛23号错，并找到下一个因子的开始，使语法分析可以继续运行下去 
        }
    }
    
    private void praseWhileStatement(int lev) {
        int cx1 = interp.arrayPtr;                                //保存判断条件操作的位置
        getsym();
       
        condition(lev);                           //分析<条件>
        int cx2 = interp.arrayPtr;                               //保存循环体的结束下一个位置
        interp.gen(Pcode.JPC, 0, 0);
        if (cursym.symtype == Symbol.dosym) {
            getsym();
        } else {
            //myErr.report(18,lex.lineCnt);                               //缺少do
        }
        statement(lev);                            //分析<语句>
        interp.gen(Pcode.JMP, 0, cx1);              //回头重新判断条件
        interp.pcodeArray[cx2].a = interp.arrayPtr;                //反填跳出循环的地址，与<条件语句>类似
    }
    private void condition(int lev) {
        if (cursym.symtype == Symbol.oddsym) {                        //分析ODD<表达式>
            getsym();
            expression(lev);
            interp.gen(Pcode.OPR, 0, 6);                        //OPR 0 6:判断栈顶元素是否为奇数
        } else {                                                           //分析<表达式><关系运算符><表达式>
           
            expression(lev);
            if (cursym.symtype == Symbol.eql || cursym.symtype == Symbol.neq
                    || cursym.symtype == Symbol.lss || cursym.symtype == Symbol.leq
                    || cursym.symtype == Symbol.gtr || cursym.symtype == Symbol.geq) {
                int relationOperatorType = cursym.symtype;                                                  //预先保存symtype的值
                getsym();
                expression(lev);
                interp.gen(Pcode.OPR, 0, relationOperatorType);                                //symtype=eql... leq与7... 13相对应
            } else {
                //myErr.report(20,lex.lineCnt);                                                                              //应为关系运算符
            }
        }
    }
    
    private void praseReadStatement(int lev) {
        getsym();
        if (cursym.symtype == Symbol.lparen) {                                            //左括号
            int index = 0;
            do {
                getsym();
                if (cursym.symtype == Symbol.ident) //标识符
                {
                    index = table.position(cursym.id);
                }
                if (index == 0) {
                    //myErr.report(35,lex.lineCnt);                                                   //read()中应是声明过的变量名                       
                } else {
                    SymbolTable.Item item = table.get(index);
                    if (item.type != SymbolTable.Item.variable) {                      //判断符号表中的该符号类型是否为变量
                        //myErr.report(32,lex.lineCnt);                                             //read()中的标识符不是变量
                    } else {
                        interp.gen(Pcode.OPR, 0, 16);                            //OPR 0 16:读入一个数据
                        interp.gen(Pcode.STO, lev - item.lev, item.addr);   //STO L A;存储变量
                    }
                }
                getsym();
            } while (cursym.symtype == Symbol.comma);
        } else {
            //myErr.report(34,lex.lineCnt);                                                          //格式错误，应是左括号
        }

        if (cursym.symtype == Symbol.rparen) //匹配成功！
        {
            getsym();
        } else {
            //myErr.report(33,lex.lineCnt);                                                          //格式错误，应是右括号
            //while (!fsys.get(cursym.symtype)) //sym.symtype!=NULL ???
            //{
            //    getsym();
            //}
        }
    }
    private void praseWriteStatement(int lev) {
        getsym();
        if (cursym.symtype == Symbol.lparen) {
            do {
                getsym();
              
                expression(lev);
                interp.gen(Pcode.OPR, 0, 14);                                     //OPR 0 14:输出栈顶的值
            } while (cursym.symtype == Symbol.comma);

            if (cursym.symtype == Symbol.rparen) //解析成功
            {
                getsym();
            } else {
               // myErr.report(33,lex.lineCnt);                                                        //格式错误，应为右括号
            }
        } else {
            //myErr.report(34,lex.lineCnt);                                                            //格式错误，应为右括号
        }
        interp.gen(Pcode.OPR, 0, 15);                                             //OPR 0 15:输出换行
    }

    private void praseCallStatement(int lev) {
        getsym();
        if (cursym.symtype == Symbol.ident) {                                  //检查符号表中该标识符是否已声明
            int index = table.position(cursym.id);
            if (index != 0) {                                                    //若table中无此名字，返回0
                SymbolTable.Item item = table.get(index);               //获得名字表某一项的内容
                if (item.type == SymbolTable.Item.procedure) //检查该标识符的类型是否为procedure
                {
                    interp.gen(Pcode.CAL, lev - item.lev, item.addr);
                } else {
                    //myErr.report(15,lex.lineCnt);                                        //call后标识符应为过程
                }
            } else {
                //myErr.report(11,lex.lineCnt);                                             //过程调用未找到
            }
            getsym();
        } else {
            //myErr.report(14,lex.lineCnt);                                                //call后应为标识符
        }
    }


  }
