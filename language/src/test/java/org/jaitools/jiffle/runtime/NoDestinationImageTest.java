/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package org.jaitools.jiffle.runtime;

import java.awt.image.RenderedImage;
import java.util.Random;

import org.jaitools.imageutils.ImageUtils;
import org.jaitools.jiffle.Jiffle;
import org.jaitools.jiffle.JiffleException;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for scripts with no destination image.
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class NoDestinationImageTest {
    
    private static final int WIDTH = 10;

    private class TestData {
        RenderedImage image;
        int expectedCount;
    }
    
    @Test
    public void countValues() throws Exception {
        System.out.println("   counting pixels that meet a condition");
        
        final int testVal = 10;
        
        String script = String.format(
                  "images { src=read; } \n"
                + "init { count = 0; } \n"
                + "count += src > %d;",
                testVal);
        
        TestData testData = createTestData(testVal);
        
        Jiffle jiffle = getCompiledJiffle(script);
        JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
        runtime.setSourceImage("src", testData.image);
        runtime.evaluateAll(null);
        
        Double count = runtime.getVar("count");
        assertNotNull(count);
        assertEquals(testData.expectedCount, count.intValue());
    }
    
    @Test(expected=JiffleException.class)
    public void noImagesAtAll() throws Exception {
        System.out.println("   no source or destination images causes exception");
        
        String script = "answer = 42;" ;
        getCompiledJiffle(script);
    }
    
    private Jiffle getCompiledJiffle(String script) throws JiffleException {
        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        
        return jiffle;
    }
    
    private TestData createTestData(int midPoint) {
        Integer[] data = new Integer[WIDTH * WIDTH];
        Random rr = new Random();
        int n = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = (int) (2 * midPoint * rr.nextDouble());
            if (data[i] > midPoint) {
                n++;
            }
        }
        
        TestData testData = new TestData();
        testData.image = ImageUtils.createImageFromArray(data, WIDTH, WIDTH);
        testData.expectedCount = n;
        return testData;
    }
}
