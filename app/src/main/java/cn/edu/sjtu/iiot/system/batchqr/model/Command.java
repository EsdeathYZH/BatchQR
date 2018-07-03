package cn.edu.sjtu.iiot.system.batchqr.model;



/**
 * Created by Administrator on 2018/4/26 0026.
 */

public class Command {
    public String[] get_text;
    public String name=null;
    public int id=-1;
    public void get_command(){
        find();
    }


    private void find(){
        int j,p1,p2,p3;
        for(j=0;j<get_text.length;j++){
            if(isFind(get_text[j])==1){
                if(j+1<get_text.length){
                    if(eng2num(get_text[j+1])==-1){//不是数字
                        if(isName(get_text[j+1])==1) {//已经找到名称并写入名字
                            break;
                        }
                        else{//直接将find后内容写入名字
                            name=get_text[j+1];
                            break;
                        }
                    }
                    else{//是数字
                        if(j+3<get_text.length && eng2num(get_text[j+2])!=-1 && eng2num(get_text[j+3])!=-1){
                            //三个数字
                            p1=eng2num(get_text[j+3]);
                            p2=eng2num(get_text[j+2]);
                            p3=eng2num(get_text[j+1]);
                            id=p3*100+p2*10+p1;
                            break;
                        }
                        if(j+2<get_text.length && eng2num(get_text[j+2])!=-1){
                            //两个数字
                            p1=eng2num(get_text[j+2]);
                            p2=eng2num(get_text[j+1]);
                            id=p2*10+p1;
                            break;
                        }
                        //一个数字
                        id=eng2num(get_text[j+1]);
                        break;
                    }
                }
                else{
                    break;
                }
            }
        }
    }

    private int isFind(String text){
        if(text.compareTo("Find") == 0)
            return 1;
        if(text.compareTo("Fight") == 0)
            return 1;
        if(text.compareTo("Found") == 0)
            return 1;
        if(text.compareTo("Don't") == 0)
            return 1;
        if(text.compareTo("Fours") == 0)
            return 1;
        if(text.compareTo("And") == 0)
            return 1;
        if(text.compareTo("Want") == 0)
            return 1;
        if(text.compareTo("Funds") == 0)
            return 1;
        if(text.compareTo("want") == 0)
            return 1;
        if(text.compareTo("Founded") == 0)
            return 1;
        if(text.compareTo("Finds") == 0)
            return 1;
        if(text.compareTo("Fine") == 0)
            return 1;
        if(text.compareTo("What") == 0)
            return 1;
        if(text.compareTo("Five") == 0)
            return 1;
        if(text.compareTo("Final") == 0)
            return 1;
        if(text.compareTo("Finally") == 0)
            return 1;
        if(text.compareTo("For") == 0)
            return 1;
        if(text.compareTo("Form") == 0)
            return 1;
        if(text.compareTo("Farm") == 0)
            return 1;
        if(text.compareTo("Fair") == 0)
            return 1;
        if(text.compareTo("Fun") == 0)
            return 1;
        if(text.compareTo("Kind") == 0)
            return 1;
        if(text.compareTo("Front") == 0)
            return 1;
        return 0;
    }
    private int isName(String text){
        if(text.compareTo("betty")==0) {
            name = "Betty";
            return 1;
        }
        if(text.compareTo("kathleen")==0) {
            name = "Kathleen";
            return 1;
        }
        if(text.compareTo("lucia")==0) {
            name = "Lucia";
            return 1;
        }
        if(text.compareTo("emily")==0) {
            name = "Emily";
            return 1;
        }
        if(text.compareTo("susan")==0) {
            name = "Susan";
            return 1;
        }
        if(text.compareTo("wendy")==0) {
            name = "Wendy";
            return 1;
        }
        if(text.compareTo("anne")==0) {
            name = "Anne";
            return 1;
        }
        if(text.compareTo("laura")==0) {
            name = "Laura";
            return 1;
        }
        if(text.compareTo("marie")==0) {
            name = "Marie";
            return 1;
        }
        if(text.compareTo("jessica")==0) {
            name = "Jessica";
            return 1;
        }
        if(text.compareTo("tina")==0) {
            name = "Tina";
            return 1;
        }
        if(text.compareTo("nancy")==0) {
            name = "Nancy";
            return 1;
        }
        if(text.compareTo("janice")==0) {
            name = "Janice";
            return 1;
        }
        if(text.compareTo("anna")==0) {
            name = "Anna";
            return 1;
        }
        if(text.compareTo("linda")==0) {
            name = "Linda";
            return 1;
        }
        if(text.compareTo("ellen")==0) {
            name = "Ellen";
            return 1;
        }
        if(text.compareTo("lisa")==0) {
            name = "Lisa";
            return 1;
        }
        if(text.compareTo("emma")==0){
            name = "Emma";
            return 1;
        }
        if(text.compareTo("mary")==0) {
            name = "Mary";
            return 1;
        }
        if(text.compareTo("tony")==0) {
            name = "Tony";
            return 1;
        }
        if(text.compareTo("twenty")==0) {
            name = "Tony";
            return 1;
        }
        if(text.compareTo("allen")==0) {
            name = "Allen";
            return 1;
        }
        if(text.compareTo("kevin")==0) {
            name = "Kevin";
            return 1;
        }
        if(text.compareTo("kelly")==0) {
            name = "Kelly";
            return 1;
        }
        if(text.compareTo("rose")==0) {
            name = "Rose";
            return 1;
        }
        if(text.compareTo("james")==0) {
            name = "James";
            return 1;
        }
        if(text.compareTo("olivia")==0) {
            name = "Olivia";
            return 1;
        }
        if(text.compareTo("sophia")==0) {
            name = "Sophia";
            return 1;
        }if(text.compareTo("charles")==0) {
            name = "Charles";
            return 1;
        }if(text.compareTo("jane")==0) {
            name = "Jane";
            return 1;
        }if(text.compareTo("william")==0) {
            name = "William";
            return 1;
        }if(text.compareTo("david")==0) {
            name = "David";
            return 1;
        }
        if(text.compareTo("richard")==0) {
            name = "Richard";
            return 1;
        }
        if(text.compareTo("daniel")==0) {
            name = "Daniel";
            return 1;
        }
        if(text.compareTo("mark")==0) {
            name = "Mark";
            return 1;
        }
        if(text.compareTo("andrew")==0) {
            name = "Andrew";
            return 1;
        }
        if(text.compareTo("jean")==0) {
            name = "Jean";
            return 1;
        }
        if(text.compareTo("vera")==0) {
            name = "Vera";
            return 1;
        }
        if(text.compareTo("john")==0) {
            name = "John";
            return 1;
        }
        if(text.compareTo("shirley")==0) {
            name = "Shirley";
            return 1;
        }
        if(text.compareTo("grace")==0) {
            name = "Grace";
            return 1;
        }
        if(text.compareTo("tom")==0) {
            name = "Tom";
            return 1;
        }
        if(text.compareTo("hannah")==0) {
            name = "Hannah";
            return 1;
        }
        if(text.compareTo("robert")==0) {
            name = "Robert";
            return 1;
        }
        if(text.compareTo("angel")==0) {
            name = "Angel";
            return 1;
        }
        if(text.compareTo("lucy")==0) {
            name = "Lucy";
            return 1;
        }
        if(text.compareTo("amy")==0) {
            name = "Amy";
            return 1;
        }
        if(text.compareTo("judy")==0) {
            name = "Judy";
            return 1;
        }
        if(text.compareTo("rachel")==0) {
            name = "Rachel";
            return 1;
        }
        if(text.compareTo("evelyn")==0) {
            name = "Evelyn";
            return 1;
        }
        if(text.compareTo("edith")==0) {
            name = "Edith";
            return 1;
        }
        if(text.compareTo("diana")==0) {
            name = "Diana";
            return 1;
        }
        if(text.compareTo("jessie")==0) {
            name = "Jessie";
            return 1;
        }
        if(text.compareTo("jason")==0) {
            name = "Jason";
            return 1;
        }
        if(text.compareTo("june")==0) {
            name = "June";
            return 1;
        }
        if(text.compareTo("gina")==0) {
            name = "Gina";
            return 1;
        }
        if(text.compareTo("julia")==0) {
            name = "Julia";
            return 1;
        }
        if(text.compareTo("carol")==0) {
            name = "Carol";
            return 1;
        }
        if(text.compareTo("alice")==0) {
            name = "Alice";
            return 1;
        }
        if(text.compareTo("beth")==0) {
            name = "Beth";
            return 1;
        }
        if(text.compareTo("bob")==0) {
            name = "Bob";
            return 1;
        }
        if(text.compareTo("helen")==0) {
            name = "Helen";
            return 1;
        }
        return 0;
    }

    private int eng2num(String text)
    {
        int result =-1;
        if(text.compareTo("zero") == 0)
            result=0;
        else  if(text.compareTo("killer") == 0)
            result=1;
        else  if(text.compareTo("jill") == 0)
            result=1;
        else  if(text.compareTo("one") == 0)
            result=1;
        else  if(text.compareTo("on") == 0)
            result=1;
        else if(text.compareTo("wine") == 0)
            result=1;
        else if(text.compareTo("what") == 0)
            result=1;
        else if(text.compareTo("why") == 0)
            result=1;
        else if(text.compareTo("two") == 0)
            result=2;
        else if(text.compareTo("to") == 0)
            result=2;
        else if(text.compareTo("too") == 0)
            result=2;
        else if(text.compareTo("three") == 0)
            result=3;
        else if(text.compareTo("sorry") == 0)
            result=3;
        else if(text.compareTo("sweet") == 0)
            result=3;
        else if(text.compareTo("four") == 0)
            result=4;
        else if(text.compareTo("for") == 0)
            result=4;
        else if(text.compareTo("fox") == 0)
            result=4;
        else if(text.compareTo("force") == 0)
            result=4;
        else if(text.compareTo("five") == 0)
            result=5;
        else if(text.compareTo("fine") == 0)
            result=5;
        else if(text.compareTo("flies") == 0)
            result=5;
        else if(text.compareTo("six") == 0)
            result=6;
        else if(text.compareTo("sex") == 0)
            result=6;
        else if(text.compareTo("seven") == 0)
            result=7;
        else if(text.compareTo("so") == 0)
            result=7;
        else if(text.compareTo("eight") == 0)
            result=8;
        else if(text.compareTo("aunt") == 0)
            result=8;
        else if(text.compareTo("eight") == 0)
            result=8;
        else if(text.compareTo("age") == 0)
            result=8;
        else if(text.compareTo("it") == 0)
            result=8;
        else if(text.compareTo("its") == 0)
            result=8;
        else if(text.compareTo("nine") == 0)
            result=9;
        else if(text.compareTo("line") == 0)
            result=9;
        else if(text.compareTo("like") == 0)
            result=9;
        else if(text.compareTo("bye") == 0)
            result=9;
        else if(text.compareTo("my") == 0)
            result=9;
        else if(text.compareTo("night") == 0)
            result=9;
        return result;
    }
}
