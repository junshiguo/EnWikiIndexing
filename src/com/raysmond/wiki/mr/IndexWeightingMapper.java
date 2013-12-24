package com.raysmond.wiki.mr;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.raysmond.wiki.util.CounterUtil;
import com.raysmond.wiki.util.StringUtils;
import com.raysmond.wiki.writable.WordIndex;
import com.raysmond.wiki.writable.WordIndexWithoutPosition;

/**
 * IndexMapper class. The map will receive a page in XML form, and resolve it
 * into words and inverted indexes. The word index in the page contains the
 * current page ID and all positions where it appears in term of word offset.
 * 
 * @author Raysmond, Junshi Guo
 */
public class IndexWeightingMapper extends
		Mapper<LongWritable, Text, Text, WordIndexWithoutPosition> {

	// Word to index map
	private HashMap<String, WordIndexWithoutPosition> result;
	
	public final static int MAX_WORD_LENGTH = 255;
	
	private static final Pattern REF = Pattern.compile("<ref>.*?</ref>");
	private static final Pattern LANG_LINKS = Pattern.compile("\\[\\[[a-z\\-]+:[^\\]]+\\]\\]");
	private static final Pattern DOUBLE_CURLY = Pattern.compile("\\{\\{.*?\\}\\}");
	private static final Pattern URL = Pattern.compile("http://[^ <]+"); 
	private static final Pattern HTML_TAG = Pattern.compile("<[^!][^>]*>");
	private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	
	/**
	 * Map method
	 * 
	 * @param value
	 *            Text a wiki page in XML form
	 */
	public void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		String id = parseXMLTag("id", value);
		String text = getPlainText(parseXMLTag("title", value) + "\n" + parseXMLText(value));
		CounterUtil.countPage();
		
		result = new HashMap<String, WordIndexWithoutPosition>();

		int pos = 0;
		String[] words = text.split("[\\s+|[\\p{Punct}]+]+");
		for (String word : words) {
			CounterUtil.updateMaxWordLength(word);
			if (word.length() <= MAX_WORD_LENGTH) 
				addWord(id, word, pos++);
		}

		Iterator<String> it = result.keySet().iterator();
		while (it.hasNext()) {
			String word = it.next();
			context.write(new Text(word.toLowerCase()), result.get(word));
		}
	}

	/**
	 * Add a word in a page and update the indexes.
	 * 
	 * @param articleId
	 *            page ID
	 * @param word
	 *            a word in page
	 * @param position
	 *            word offset in the page
	 */
	public void addWord(String articleId, String word, Integer position) {
		WordIndexWithoutPosition output = result.get(word);
		if (output != null) {
			output.addPosition(position);
		} else {
			output = new WordIndexWithoutPosition(articleId);
			output.addPosition(position);
			result.put(word, output);
		}
	}

	/**
	 * Parse article XML
	 * 
	 * @param tag
	 * @param article
	 * @return
	 * @throws CharacterCodingException
	 */
	private String parseXMLTag(String tag, Text article)
			throws CharacterCodingException {
		int start = article.find("<" + tag + ">");
		int end = article.find("</" + tag + ">");

		if (start == -1 || end == -1) {
			return "";
		} else {
			start += tag.length() + 2;
			return Text.decode(article.getBytes(), start, end - start);
		}
	}

	/**
	 * Parse article content
	 * 
	 * @param article
	 * @return
	 * @throws CharacterCodingException
	 */
	private String parseXMLText(Text article) throws CharacterCodingException {
		String tag = "<text xml:space=\"preserve\">";
		int start = article.find("<text xml:space=\"preserve\">");
		int end = article.find("</text>");

		if (start == -1 || end == -1) {
			return "";
		} else {
			start += tag.length();
			return Text.decode(article.getBytes(), start, end - start);
		}
	}

	/**
	 * Get plain text from XML text
	 * 
	 * @param text
	 * @return
	 */
	private String cleanText(String text) {
		// text = EntityDecoder.entityToHtml(text);
		text = StringUtils.unescapeXML(text);

		text = text.replaceAll("[=]+[A-Za-z+\\s-]+[=]+", " ")
				.replaceAll("\\{\\{[A-Za-z0-9+\\s-]+\\}\\}", " ")
				.replaceAll("(?m)<ref>.+</ref>", " ")
				.replaceAll("(?m)<ref name=\"[A-Za-z0-9\\s-]+\">.+</ref>", " ")
				.replaceAll("<ref>", " <ref>");

		// Convert to plain text
		WikiModel wikiModel = new WikiModel("${image}", "${title}");

		// Remove text inside {{ }}
		String plainStr = wikiModel.render(new PlainTextConverter(), text)
				.replaceAll("\\{\\{[A-Za-z+\\s-]+\\}\\}", " ");

		return plainStr;
	}

	private String getPlainText(String s) {
		WikiModel wikiModel = new WikiModel("", "");
		PlainTextConverter textConverter = new PlainTextConverter();

		// Bliki doesn't seem to properly handle inter-language links, so remove
		// manually.
		s = LANG_LINKS.matcher(s).replaceAll(" ");

		wikiModel.setUp();
		s = wikiModel.render(textConverter, s);
		wikiModel.tearDown();

		// The way the some entities are encoded, we have to unescape twice.
		s = StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeHtml(s));

		s = REF.matcher(s).replaceAll(" ");
		s = HTML_COMMENT.matcher(s).replaceAll(" ");

		// Sometimes, URL bumps up against comments e.g., <!--
		// http://foo.com/-->
		// Therefore, we want to remove the comment first; otherwise the URL
		// pattern might eat up
		// the comment terminator.
		s = URL.matcher(s).replaceAll(" ");
		s = DOUBLE_CURLY.matcher(s).replaceAll(" ");
		s = HTML_TAG.matcher(s).replaceAll(" ");

		return s;
	}

}
