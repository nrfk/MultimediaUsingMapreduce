package fr.telecomParistech.example.pipeline;

import java.util.LinkedList;
import java.util.List;

import com.google.appengine.tools.pipeline.FutureValue;
import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;

public class InverseSentenceExample {
	public static class InverseSentence extends Job1<String, String> {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3941661655744725202L;

		@Override
		public Value<String> run(String inputSentence) {
			// TODO Auto-generated method stub
			String[] words = inputSentence.split("^[a-zA-Z]");
			List<FutureValue<String>> inversedWords = 
					new LinkedList<FutureValue<String>>();
			for (String word : words) {
				inversedWords.add(
						futureCall(new InverseWordJob(), immediate(word)));
			}
			return futureCall(new InverseSentenceJob(), 
					futureList(inversedWords));
		}
		
	}
	
	public static class InverseWordJob extends Job1<String, String> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8324263770563202026L;

		@Override
		public Value<String> run(String inputWord) {
			// TODO Auto-generated method stub
			String outputWord = "";
			for (int i = inputWord.length() - 1; i >= 0 ; i--) {
				outputWord += inputWord.charAt(i);
			}
			return immediate(outputWord);
		}
		
	}
	
	public static class InverseSentenceJob extends Job1<String, List<String>> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2596137553742902965L;

		@Override
		public Value<String> run(List<String> inversedWords) {
			// TODO Auto-generated method stub
			String inversedSentence = "";
			for (int i = inversedWords.size() - 1; i >= 0; i-- ) {
				inversedSentence += inversedWords.get(i);
			}
			return immediate(inversedSentence);
		}
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
