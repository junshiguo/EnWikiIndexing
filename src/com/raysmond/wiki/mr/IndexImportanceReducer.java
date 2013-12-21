package com.raysmond.wiki.mr;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;

import com.raysmond.wiki.writable.IndexList;
import com.raysmond.wiki.writable.WordIndex;

/**
 * IndexImportanceReducer class
 * Sort all page ID by term weighting
 * Term weighting = TF(i,j) * log(N/n(i))
 * @author Raysmond
 *
 */
public class IndexImportanceReducer extends TableReducer<Text, WordIndex, Text> {
	private TreeMap<String, WordIndex> map;

	@Override
	public void reduce(Text key, Iterable<WordIndex> values, Context context)
			throws IOException, InterruptedException {
		map = new TreeMap<String, WordIndex>();

		Iterator<WordIndex> it = values.iterator();
		while (it.hasNext()) {
			WordIndex index = it.next();
			String aid = index.getArticleId();
			if (map.get(aid) == null) {
				// deep copy the object, or the values will all be same
				map.put(aid, new WordIndex(index));
			}
		}

	   // System.out.println(key.toString() + ": " +(new IndexList(map)).toString());
		Put put = new Put(key.getBytes());
		put.add(Bytes.toBytes("content"), Bytes.toBytes("index"), Bytes.toBytes((new IndexList(map)).toString()));
		context.write(new Text(key), put);
	}
}