/*
 * (c) Copyright IBM Corp. 2005 All Rights Reserved.
 */
package org.dita.dost.reader;

import java.util.Locale;
import java.util.Stack;

import org.dita.dost.index.IndexTerm;
import org.dita.dost.index.IndexTermCollection;
import org.dita.dost.index.IndexTermTarget;
import org.dita.dost.util.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This class extends SAX's DefaultHandler, used for parse index term from dita
 * files.
 * 
 * @version 1.0 2005-04-30
 * 
 * @author Wu, Zhi Qiang
 */
public class IndexTermReader extends AbstractXMLReader {
	/** The target file under parsing */
	private String targetFile = null;

	/** The title of the dita file under parsing */
	private String title = null;

	/** If current element under parsing is a title element */
	private boolean inTitleElement = false;

	/** title element found? */
	private boolean isTitleFound = false;

	/** Stack used to store index term */
	private Stack termStack = null;

	/**
	 * Constructor
	 */
	public IndexTermReader() {
		termStack = new Stack();
	}

	/**
	 * Reset the reader.
	 */
	public void reset() {
		targetFile = null;
		title = null;
		inTitleElement = false;
		isTitleFound = false;
		termStack.clear();
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String temp = new String(ch, start, length).trim();

		/*
		 * For title info
		 */
		if (inTitleElement && temp.length() > 0) {
			title = (title == null) ? temp : new StringBuffer(title).append(
					temp).toString();
		}

		if (!termStack.empty()) {
			IndexTerm indexTerm = (IndexTerm) termStack.peek();

			if (temp.length() == 0) {
				return;
			}

			if (indexTerm.getTermName() == null) {
				indexTerm.setTermName(temp);
			} else {
				indexTerm.setTermName(new StringBuffer(indexTerm.getTermName())
						.append(temp).toString());
			}
		}
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (Constants.ELEMENT_NAME_INDEXTERM.equals(localName)) {
			IndexTerm term = (IndexTerm) termStack.pop();

			if (term.getTermName() == null) {
				term.setTermName("***");
				System.out
						.println("The indexterm element does not have any content. Setting the term to ***.");
			}

			if (termStack.empty()) {
				IndexTermCollection.addTerm(term);
			} else {
				IndexTerm parentTerm = (IndexTerm) termStack.peek();
				parentTerm.addSubTerm(term);
			}
		}

		/*
		 * For title info
		 */
		if (Constants.ELEMENT_NAME_TITLE.equals(localName)
				&& isTitleFound == false) {
			isTitleFound = true;
			inTitleElement = false;
		}
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (Constants.ELEMENT_NAME_INDEXTERM.equals(localName)) {
			IndexTerm indexTerm = new IndexTerm();
			IndexTermTarget target = new IndexTermTarget();

			if (title != null) {
				target.setTargetName(title);
			} else {
				target.setTargetName(targetFile);
			}

			target.setTargetURI(targetFile);
			indexTerm.addTarget(target);
			termStack.push(indexTerm);
		}

		if (IndexTerm.getTermLocale() == null) {
			String xmlLang = attributes
					.getValue(Constants.ATTRIBUTE_NAME_XML_LANG);

			if (xmlLang != null) {
				Locale locale = new Locale(xmlLang.substring(0, 1), xmlLang
						.substring(3, 4));
				IndexTerm.setTermLocale(locale);
			}
		}

		/*
		 * For title info
		 */
		if (Constants.ELEMENT_NAME_TITLE.equals(localName)
				&& isTitleFound == false) {
			inTitleElement = true;
		}
	}

	/**
	 * Set the current parsing file
	 * 
	 * @param target
	 *            The parsingFile to set.
	 */
	public void setTargetFile(String target) {
		this.targetFile = target;
	}

}