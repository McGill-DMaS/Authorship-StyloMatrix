package ca.mcgill.sis.dmas.nlp.model.embedding;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Set;


public class TokenCounter {
	
	private TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
	
 
   public void feed(String token, int count){
	   int value = map.get(token);
	   map.put(token, value + count);
   }
   
   public void feed(String token){
	  if(!map.increment(token)){
		  map.put(token, 1);
	  }
   }

    public int get(String token){
        return map.get(token);
    }

  
    public int size() {
        return map.size();
    }
 
    
    public Set<String> keySet(){
        return map.keySet();
    }
    
    public static void main(String [] args){
    	TokenCounter tCounter = new TokenCounter();
    	for(int i = 0; i < 6; ++i){
    		tCounter.feed(Integer.toString(i));
    		for(int j =0; j < 6; ++j){
    			tCounter.feed(Integer.toString(j));
    		}
    	}
    	
    	for (String string : tCounter.keySet()) {
			System.out.println(string + " " + tCounter.get(string));
		}
    }
}
