package fr.telecomParistech.mapreduce;

import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;

/**
 * This is the Reducer function of the Map-Reduce extractor.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ImageExtractorReducer extends Reducer<Integer, String, String> {
	private static final long serialVersionUID = 3748003219458311578L;

	@Override
	public void reduce(Integer key, ReducerInput<String> values) {
		StringBuffer result = new StringBuffer();
		result.append("Representation: \n");
		int i = 0;
		while (values.hasNext()) {
			i++;
			String url = values.next();
			result.append(i + ". " + url + "\n");
		}
		System.out.println("Reducer *******************");
		System.out.println(result.toString());
		getContext().emit(result.toString());
	}
}
