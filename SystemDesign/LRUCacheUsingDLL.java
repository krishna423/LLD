package SystemDesign;

import java.util.HashMap;
import java.util.Map;

class LRUCacheUsingDLL {

    //unordered_map<int, Node> mp;
    Map<Integer, Node> mp=new HashMap<Integer,Node>();
    int capacity;

    //Head have new node
    //Tail have previously used element

    Node head =new Node(0,0);
    Node tail =new Node(0,0);

    public LRUCacheUsingDLL(int capacity) {
        this.capacity=capacity;
        head.next= tail;
        tail.prev= head;
    }

    public void insert(Node newNode){
        newNode.next= head.next;
        head.next.prev=newNode;
        head.next=newNode;
        newNode.prev= head;
        mp.put(newNode.key,newNode);
        //mp[cur.key]=cur;
    }

    public void remove(Node cur){
        mp.remove(cur.key);
        cur.next.prev=cur.prev;
        cur.prev.next=cur.next;
        cur.next=null;
        cur.prev=null;
    }


    public int get(int key) {
        if(mp.get(key)==null){
            System.out.println("Get " + key  + " "  + -1);
            return -1;
        }

        Node cur=mp.get(key);
        //Move to head
        remove(cur);
        insert(cur);
        System.out.println("Get " + cur.key + " " + cur.val);
        return cur.val;
    }

    public void put(int key, int val) {
        //If key already present then remove
        //If queue capacity is filled then last node
        if(mp.get(key)!=null) remove(mp.get(key));

        if(mp.size()==capacity) remove(tail.prev);

        insert(new Node(key,val));
        print();
    }

    public class Node{

        int key;
        int val;
        Node next;
        Node prev;

        public Node(int key, int val){
            this.key=key;
            this.val=val;
            this.next=null;
            this.prev=null;
        }
    }

    void print(){
        Node headNode = head.next;

        System.out.print("Put ");
        while(headNode!=tail){
            System.out.print(headNode.key + " ");
            headNode = headNode.next;
        }
        System.out.println("");
    }

    public static void main(String[] args) {
        LRUCacheUsingDLL lRUCache = new LRUCacheUsingDLL(2);
        lRUCache.put(1, 1); // cache is {1=1}
        lRUCache.put(2, 2); // cache is {1=1, 2=2}
        lRUCache.get(1);    // return 1
        lRUCache.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
        lRUCache.get(2);    // returns -1 (not found)
        lRUCache.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
        lRUCache.get(1);    // return -1 (not found)
        lRUCache.get(3);    // return 3
        lRUCache.get(4);    // return 4
    }
}
