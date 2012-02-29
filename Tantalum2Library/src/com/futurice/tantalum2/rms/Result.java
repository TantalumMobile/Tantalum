/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.futurice.tantalum2.rms;

/**
 * For asynchronous cache get. Non-blocking get results are returned in a runnable
 * object, often of the form:
 * 
 * staticWebCache.get("myurl", new Result() {
 *     public void run() {
 *         // do something on EDT with getResult() object
 *     }
 * 
 * The run() method will be automatically completed on the EDT thread after
 * changes in supporting methods in the Tantalum library.
 * 
 * Be sure to use volatile or synchronized() to stay thread safe
 * 
 * @author tsaa
 */
public interface Result extends Runnable {
    public Object getResult();
    
    public void setResult(Object result);
}