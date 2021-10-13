package songwu.deds.trajectory.utility;

import java.util.ArrayList;
import java.util.List;

public class MyCollection <T>{
    public List<T> flat(List<List<T>> lists){
        List<T> answer = new ArrayList<>();
        for(List<T> list : lists){
            answer.addAll(list);
        }
        return answer;
    }

    public static void main(String[] args) {
        MyCollection<Integer> is = new MyCollection<>();
        List<List<Integer>> lists = new ArrayList<>();
        List<Integer> l1 = new ArrayList<>(); l1.add(1); l1.add(2);
        List<Integer> l2 = new ArrayList<>(); l2.add(10); l2.add(20);
        lists.add(l1); lists.add(l2);
        for(Integer i : is.flat(lists))
            System.out.println(i);
    }
}
