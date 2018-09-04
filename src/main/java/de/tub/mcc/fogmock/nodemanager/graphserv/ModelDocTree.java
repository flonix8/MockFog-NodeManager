package de.tub.mcc.fogmock.nodemanager.graphserv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class ModelDocTree {

    public ModelDoc[] allDocs;
    public Map<Integer, Integer[]> tMap;


//    @JsonIgnore
//    public ModelDocTree(ModelDoc modelDoc) throws ExceptionInvalidData {
//        this.allDocs = new ModelDoc[]{modelDoc};
//        this.tMap = new HashMap<Integer, Integer[]>();
//        tMap.put(0, new Integer[]{});
//    }


    @JsonCreator
    public ModelDocTree(@JsonProperty("allDocs") ModelDoc[] allDocs, @JsonProperty("tMap") Map<Integer, Integer[]> tMap) throws ExceptionInvalidData {
        this.allDocs = allDocs;
        this.tMap = tMap;

        if (allDocs == null || allDocs.length == 0) throw new ExceptionInvalidData("missing documents");
        //if (tMap == null || tMap.size() == 0) throw new ExceptionInvalidData("missing tMap");
        if (tMap == null) tMap = new HashMap<Integer, Integer[]>();
        if (tMap.size() == 0) tMap.put(0, new Integer[]{});
        if (tMap.size() != allDocs.length) throw new ExceptionInvalidData("illegal tMap: inappropriate number of keys");



        //throw error if tMap is partly reflexive
//        if ( !tMap.entrySet().stream().allMatch(e->Arrays.stream(e.getValue()).allMatch(f->!f.equals(e.getKey()))) ) {
//            throw new ExceptionInvalidData("illegal tMap: loops detected ");
//        }

        if ( tMap.values().stream().flatMap(Stream::of).anyMatch(x->x>=allDocs.length || x<0) ) {
            throw new ExceptionInvalidData("illegal tMap: value index out of range");
        }
        if ( tMap.keySet().stream().anyMatch(x->x>=allDocs.length || x<0) ) {
            throw new ExceptionInvalidData("illegal tMap: key index out of range");
        }

//        //throw error if the set of tMap values is not equal to {1, 2, ... allDocs.length}
//        if ( tMap.values().stream().flatMap(Stream::of).distinct().count() != allDocs.length-1 ||
//                tMap.values().stream().flatMap(Stream::of).count() != allDocs.length-1 ) {
//            throw new ExceptionInvalidData("illegal tree map: inappropriate number of values");
//        }
//
//        //throw error if there are not exactly allDocs.length tMap keys
//        if ( tMap.keySet().stream().distinct().count() != allDocs.length) {
//            throw new ExceptionInvalidData("illegal tree map: inappropriate number of keys");
//        }

        List<Integer> range = IntStream.rangeClosed(0, allDocs.length).boxed().collect(Collectors.toList());
        searchHelper(0, range);
        if (range.size() > 1) throw new ExceptionInvalidData("illegal tMap: broken connectivity: "+Arrays.toString(range.toArray()));

    }


    private void searchHelper(Integer current, List<Integer> range) throws ExceptionInvalidData {
        if (!range.remove(current)) throw new ExceptionInvalidData("illegal tMap: entry not unique or part of a loop: "+current);
        for (Integer next : tMap.get(current) ) {
            searchHelper(next, range);
        }
    }

}
