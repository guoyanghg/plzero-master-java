package exp;

import java.util.Arrays;

public class WordAnaly {
	 private char curCh = ' ';
	 private String program;
	 public int chCount = 0;
	 private int[] ssym;
	 public WordAnaly(String p) {
		// TODO Auto-generated constructor stub
		    ssym = new int[256];
	        Arrays.fill(ssym, Symbol.nul);
	        ssym['+'] = Symbol.plus;
	        ssym['-'] = Symbol.minus;
	        ssym['*'] = Symbol.mul;
	        ssym['/'] = Symbol.div;
	        ssym['('] = Symbol.lparen;
	        ssym[')'] = Symbol.rparen;
	        ssym['='] = Symbol.eql;
	        ssym[','] = Symbol.comma;
	        ssym['.'] = Symbol.peroid;
	        ssym[';'] = Symbol.semicolon;
	        program=p;
	}
	 public void getch() {
	        if (chCount < program.length()) {
	        	
	          curCh = program.charAt(chCount++);
	           
	        }
	    }
	 public Symbol getsym() {
	        Symbol sym;
	        while (curCh == ' ') {
	            getch();
	        }
	        if ((curCh >= 'a' && curCh <= 'z')||(curCh >= 'A' && curCh <= 'Z')) {
	            sym = matchKeywordOrIdentifier();                                     //关键字或者一般标识符
	        } else if (curCh >= '0' && curCh <= '9') {
	            sym = matchNumber();                                                       //数字
	        } else {
	            sym = matchOperator();                                                     //操作符
	        }
	        return sym;
	    }
	 private Symbol matchKeywordOrIdentifier() {
	        StringBuffer sb = new StringBuffer();
	        do{
	            sb.append(curCh);
	            getch();
	        }while((curCh >= 'a' && curCh <= 'z')||(curCh>='A'&&curCh<='Z') || (curCh >= '0' && curCh <= '9'));

	        String token = sb.toString();
	        int index = Arrays.binarySearch(Symbol.word, token);                           //搜索是不是保留字
	        Symbol sym = null;
	        if (index < 0) {
	            sym = new Symbol(Symbol.ident);                                            //一般标识符
	            sym.id = token;
	        } else {
	            sym = new Symbol(Symbol.wsym[index]);                                    //保留字对应的符号值0-31
	        }
	        return sym;
	    }

	    private Symbol matchNumber() {
	        //统计数字位数
	        Symbol sym = new Symbol(Symbol.number);
	        do {                      
	            sym.num = 10 * sym.num + curCh - '0';                                    // 获取数字的值
	            getch();
	        } while (curCh >= '0' && curCh <= '9');                                    //!!!

	        return sym;
	    }

	    private Symbol matchOperator() {
	        Symbol sym = null;
	        switch (curCh) {
	            case ':':                                                                       // 赋值符号
	                getch();
	                if (curCh == '=') {
	                    sym = new Symbol(Symbol.becomes);
	                    getch();
	                } else {
	                    sym = new Symbol(Symbol.nul);                               //不能识别的符号
	                }
	                break;
	            case '<':                                                                    //小于或者小于等于
	                getch();
	                if (curCh == '=') {
	                    sym = new Symbol(Symbol.leq);                             //是<=
	                    getch();
	                } else if (curCh == '>') {
	                    sym = new Symbol(Symbol.neq);                           //是<>
	                    getch();
	                } else {
	                    sym = new Symbol(Symbol.lss);                             //是<
	                }
	                break;
	            case '>':                                                      //大于或者大于等于
	                getch();
	                if (curCh == '=') {
	                    sym = new Symbol(Symbol.geq);                           //大于等于
	                    getch();
	                } else {
	                    sym = new Symbol(Symbol.gtr);                            //大于
	                }
	                break;
	            default:
	                sym = new Symbol(ssym[curCh]);
	                if (sym.symtype != Symbol.peroid) {
	                    getch();
	                }
	        }
	        return sym;
	    }

}
