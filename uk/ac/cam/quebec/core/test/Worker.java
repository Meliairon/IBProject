/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.cam.quebec.core.test;

/**
 *This is a dummy class that represents a worker thread and the object it is
 * working on
 * @author James
 */
public class Worker extends Thread implements Comparable{
    private final WorkerType type;
    public Worker (WorkerType _type)
    {
        type = _type;
    }
    public WorkerType getWorkerType()
    {
        return type;
    }
    public void process (Object o)
    {
        
    }
    @Override
    public int compareTo(Object o) {
       return 0;
    }
}