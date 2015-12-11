/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 * 
 */
package org.wltea.analyzer.dic;

import org.apache.lucene.analysis.util.CharArraySet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {

	/*
    * 分词器默认字典路径
    */
	private static final String PATH_DIC_MAIN = "org/wltea/analyzer/dic/main2012.dic";
	private static final String PATH_DIC_QUANTIFIER = "org/wltea/analyzer/dic/quantifier.dic";
	/*
	 * 词典单子实例
	 */
	private static Dictionary singleton;
	
	/*
	 * 主词典对象
	 */
	private DictSegment _MainDict = new DictSegment((char)0);
	
	/*
	 * 停止词词典 
	 */
	private DictSegment _StopWordDict = new DictSegment((char)0);
	/*
	 * 量词词典
	 */
	private DictSegment _QuantifierDict = new DictSegment((char)0);
	
	private Dictionary(CharArraySet words, CharArraySet stop_words){
		this.loadMainDict();
		this.loadQuantifierDict();
		this.loadWords(words, _MainDict);
		this.loadWords(stop_words, _StopWordDict);
	}

	/**
	 * 填充词典
	 * @param words
	 * @param dictSegment
     */
	private void loadWords(CharArraySet words, DictSegment dictSegment) {
		if (words != null && !words.isEmpty()) {
			Iterator i$ = words.iterator();
			while (i$.hasNext()) {
				Object item = i$.next();
				if (item instanceof char[]) {
					dictSegment.fillSegment((char[]) item);
				}
			}
		}
	}

	/**
	 * 词典初始化
	 * 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
	 * 只有当Dictionary类被实际调用时，才会开始载入词典，
	 * 这将延长首次分词操作的时间
	 * 该方法提供了一个在应用加载阶段就初始化字典的手段
	 * @return Dictionary
	 */
	public static Dictionary initial(CharArraySet words, CharArraySet stop_words){
		if(singleton == null){
			synchronized(Dictionary.class){
				if(singleton == null){
					singleton = new Dictionary(words, stop_words);
					return singleton;
				}
			}
		}
		return singleton;
	}
	
	/**
	 * 获取词典单子实例
	 * @return Dictionary 单例对象
	 */
	public static Dictionary getSingleton(){
		if(singleton == null){
			throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
		}
		return singleton;
	}
	
	/**
	 * 批量加载新词条
	 * @param words Collection<String>词条列表
	 */
	public void addWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量加载词条到主内存词典中
					singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 批量移除（屏蔽）词条
	 * @param words
	 */
	public void disableWords(Collection<String> words){
		if(words != null){
			for(String word : words){
				if (word != null) {
					//批量屏蔽词条
					singleton._MainDict.disableSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray){
		return singleton._MainDict.match(charArray);
	}
	
	/**
	 * 检索匹配主词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray , int begin, int length){
		return singleton._MainDict.match(charArray, begin, length);
	}
	
	/**
	 * 检索匹配量词词典
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray , int begin, int length){
		return singleton._QuantifierDict.match(charArray, begin, length);
	}
	
	
	/**
	 * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
	 * @param charArray
	 * @param currentIndex
	 * @param matchedHit
	 * @return Hit
	 */
	public Hit matchWithHit(char[] charArray , int currentIndex , Hit matchedHit){
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1 , matchedHit);
	}
	
	
	/**
	 * 判断是否是停止词
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray , int begin, int length){			
		return singleton._StopWordDict.match(charArray, begin, length).isMatch();
	}

	/**
	 * 加载默认词典
	 */
	private void loadMainDict(){
		//读取默认词典文件
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(PATH_DIC_MAIN);
		if(is == null){
			throw new RuntimeException("Main Dictionary not found!!!");
		}
		loadDict(is,_MainDict);
	}
	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict(){
		//读取量词词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(PATH_DIC_QUANTIFIER);
        if(is == null){
        	throw new RuntimeException("Quantifier Dictionary not found!!!");
        }
		loadDict(is,_QuantifierDict);
	}

	/**
	 * 词典文件流读入词典对象
	 * @param is
	 * @param dictSegment
     */
	private void loadDict(InputStream is, DictSegment dictSegment) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					dictSegment.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			} while (theWord != null);

		} catch (IOException ioe) {
			System.err.println("Dictionary loading exception.");
			ioe.printStackTrace();

		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
