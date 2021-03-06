package vu.wntools.predicatematrix;

import vu.wntools.util.Util;
import vu.wntools.wordnet.WordnetData;
import vu.wntools.wordnet.WordnetLmfSaxParser;

import java.io.*;
import java.util.*;

/**
 * Created by piek on 7/17/14.
 *
 * Input is a predicate matrix file (PM) with mappings across verbnet, propbank, nombank, fraqmnet and wordnet
 * We read a wordnet lmf file and for each synset with equivalences we find the corresponding PM mappings and output a new PM for the wordnet lmf synsets
 *
 * - direct equivalences: equivalence relations = eq_synonym
 * - near equivalences: equivalence relation = eq_near _synonym
 * - xpos equivalences: equivalence relation = eq_xpos_near_synonym
 * - parent equivalences: equivalence relation = eq_has_hyperonym
 * - parent xpos equivalences: equivalence relation = eq_pos_has_hyperonym
 *
 *
 * For those synsets in wordnet lmf that do not get a mapping, we traverse the hypernym chain upwards to find a synset that has a mapping.
 * We then create a child mapping to the hypernym mapping
 *
 *
 */
public class ProjectPredicateMatrix {
    static HashMap<String, ArrayList<ArrayList<String>>> wordNetPredicateMap = new HashMap<String,ArrayList<ArrayList<String>>>();
    static ArrayList<String> preEmpted = new ArrayList<String>();
    static boolean REDUCE = false;
    static boolean LU = false;

    static public void main (String[] args) {
        //String pathToPredicateMatrixFile = args[0];
        //String pathToCdbSynsetFile = args[1];
        String pathToPredicateMatrixFile = "";
        //pathToPredicateMatrixFile = "/Code/vu/WordnetTools/resources/PredicateMatrix_withESO.v0.2.txt.role";
        //String pathToPredicateMatrixFile = "/Code/vu/WordnetTools/resources/PredicateMatrix.v1.1/PredicateMatrix.v1.1.role.en";
        //String pathToPredicateMatrixFile = "/Tools/ontotagger-v1.0/resources/predicate-matrix/PredicateMatrix.v0.txt";
        String pathToWordnetLmfFile = "";
       // pathToWordnetLmfFile = "/Tools/wordnet-tools.0.1/resources/cornetto2.1.lmf.xml";
       // pathToWordnetLmfFile = "/Code/vu/WordnetTools/resources/wn-bul-lmf.xml";
        ArrayList<String> relations = new ArrayList<String>();
//        relations.add("hype");
/*
        relations.add("near_synonym");
        relations.add("xpos_synonym");
        relations.add("xpos_near_synonym");
        relations.add("xpos_near_hyperonym");
        relations.add("xpos_has_hyperonym");
        relations.add("event");
*/
        //String pathToWordnetLmfFile = "/Code/vu/WordnetTools/resources/odwn1.0.lmf";
        String wordnetName = "";
      //  wordnetName = "odwn";
      //  wordnetName = "bul";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--matrix") && ((i+1)<args.length)) {
                pathToPredicateMatrixFile = args[i+1];
            }
            else if (arg.equalsIgnoreCase("--wn-lmf") && ((i+1)<args.length)) {
                pathToWordnetLmfFile = args[i+1];
            }
            else if (arg.equalsIgnoreCase("--wn-name") && ((i+1)<args.length)) {
                wordnetName = args[i+1];
            }
            else if (arg.equalsIgnoreCase("--reduce")) {
                REDUCE = true;
            }
            else if (arg.equalsIgnoreCase("--lu")) {
                LU = true;
            }
            else if (arg.equalsIgnoreCase("--relations") && ((i+1)<args.length)) {
                relations = Util.readRelationsFile(args[i+1]);
            }
            else {
              //  System.out.println("arg = " + arg);
            }
        }
        System.out.println("pathToPredicateMatrixFile = " + pathToPredicateMatrixFile);
        System.out.println("pathToWordnetLmfFile = " + pathToWordnetLmfFile);

        processMatrixFileWithWordnetSynset(pathToPredicateMatrixFile);
        WordnetLmfSaxParser wordnetLmfSaxParser = new WordnetLmfSaxParser();
        wordnetLmfSaxParser.setRelations(relations);
        wordnetLmfSaxParser.parseFile(pathToWordnetLmfFile);
        wordnetLmfSaxParser.wordnetData.buildSynsetIndex();

      //  wordnetLmfSaxParser.wordnetData.buildDirectEquivalencesFromIds("bul-10", "eng-30");
        wordnetLmfSaxParser.wordnetData.buildDirectEquivalencesFromIds("eng-30", "eng-30");
        System.out.println("wordnetLmfSaxParser.wordnetData.hyperRelations.size; = " + wordnetLmfSaxParser.wordnetData.hyperRelations.size());
        System.out.println("wordnetLmfSaxParser.wordnetData.otherRelations.size; = " + wordnetLmfSaxParser.wordnetData.otherRelations.size());
        System.out.println("DirectEquiSynsets().size() = " + wordnetLmfSaxParser.wordnetData.getSynsetToDirectEquiSynsets().size());
        System.out.println("NearEquiSynsets().size() = " + wordnetLmfSaxParser.wordnetData.getSynsetToNearEquiSynsets().size());
        System.out.println("OtherEquiSynsets().size() = " + wordnetLmfSaxParser.wordnetData.getSynsetToOtherEquiSynsets().size());
        System.out.println("wordNetPredicateMap.size() = " + wordNetPredicateMap.size());


        HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapDirect = createMapping(wordnetLmfSaxParser.wordnetData.getSynsetToDirectEquiSynsets());
        String key = wordnetName+"-eq_synonym";
        if (REDUCE) {
           // outputReducedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapDirect, pathToPredicateMatrixFile+"."+key, wordnetName, key);
            outputMergedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapDirect, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
        else {
            outputMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapDirect, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
        HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapNear =createMapping(wordnetLmfSaxParser.wordnetData.getSynsetToNearEquiSynsets());
        key = wordnetName+"-eq_near_synonym";
        if (REDUCE) {
            //outputReducedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapNear, pathToPredicateMatrixFile+"."+key, wordnetName, key);
            outputMergedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapNear, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
        else {
            outputMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapNear, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
        HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapOther =createMapping(wordnetLmfSaxParser.wordnetData.getSynsetToOtherEquiSynsets());
        key = wordnetName+"-eq_other";
        if (REDUCE) {
            //outputReducedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapOther, pathToPredicateMatrixFile+"."+key, wordnetName, key);
            outputMergedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapOther, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
        else {
            outputMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapOther, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
        HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapParent =createHyperonymMappings(wordnetLmfSaxParser.wordnetData,
                projectedPredicateMapDirect, projectedPredicateMapNear, projectedPredicateMapOther);
        key = wordnetName+"-eq_parent";
        if (REDUCE) {
           // outputReducedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapParent, pathToPredicateMatrixFile+"."+key, wordnetName, key);
            outputMergedMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapParent, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
        else {
            outputMappings(wordnetLmfSaxParser.wordnetData, projectedPredicateMapParent, pathToPredicateMatrixFile+"."+key, wordnetName, key);
        }
    }

    static void outputMappings (WordnetData wordnetData, HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMap, String filePath, String wordnetName, String prefix) {
        System.out.println("projectedPredicateMap = " + projectedPredicateMap.size());
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            Set keySet = projectedPredicateMap.keySet();
            Iterator keys = keySet.iterator();
            String str = "";
            while (keys.hasNext()) {
                String key = (String) keys.next();
                ArrayList<ArrayList<String>> mappings = projectedPredicateMap.get(key);
                ArrayList<String> uniqueMaps = new ArrayList<String>();
                for (int m = 0; m < mappings.size(); m++) {
                    ArrayList<String> mapping =  mappings.get(m);
                    str = prefix+":"+key;
                    String synsetString = wordnetData.getSynsetString(key);
                    str += " "+wordnetName+"-synset:"+synsetString;
                    for (int i = 0; i < mapping.size(); i++) {
                        String map = mapping.get(i);
                        str += " "+map;
                    }
                    str += "\n";
                    fos.write(str.getBytes());
                }
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    static void outputReducedMappings (WordnetData wordnetData, HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMap, String filePath, String wordnetName, String prefix) {
        System.out.println("projectedPredicateMap = " + projectedPredicateMap.size());
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            Set keySet = projectedPredicateMap.keySet();
            Iterator keys = keySet.iterator();
            String str = "";
            while (keys.hasNext()) {
                String key = (String) keys.next();
                str = prefix+":"+key;
                String synsetString = wordnetData.getSynsetString(key);
                str += " "+wordnetName+"-synset:"+synsetString;
                ArrayList<ArrayList<String>> mappings = projectedPredicateMap.get(key);
                ArrayList<String> uniqueMaps = new ArrayList<String>();
                for (int m = 0; m < mappings.size(); m++) {
                    ArrayList<String> mapping =  mappings.get(m);
                    for (int i = 0; i < mapping.size(); i++) {
                        String map = mapping.get(i);
                        if (!uniqueMaps.contains(map)) {
                            uniqueMaps.add(map);
                        }
                    }
                }
                for (int i = 0; i < uniqueMaps.size(); i++) {
                    String map = uniqueMaps.get(i);
                    str += " "+map;
                }
                str += "\n";
                fos.write(str.getBytes());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    static void outputMergedMappings (WordnetData wordnetData, HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMap, String filePath, String wordnetName, String prefix) {
        System.out.println("projectedPredicateMap = " + projectedPredicateMap.size());
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            Set keySet = projectedPredicateMap.keySet();
            Iterator keys = keySet.iterator();
            String str = "";
            String matrixString = "";
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (preEmpted.contains(key)) {
                    continue;
                }
                else {
                    preEmpted.add(key);
                }
                matrixString = prefix+":"+key;
                String synsetString = wordnetData.getSynsetString(key);
                matrixString += " "+wordnetName+"-synset:"+synsetString;
                ArrayList<ArrayList<String>> mappings = projectedPredicateMap.get(key);
                HashMap<String, ArrayList<String>> frameMaps = new HashMap<String, ArrayList<String>>();
                for (int m = 0; m < mappings.size(); m++) {
                    ArrayList<String> mapping =  mappings.get(m);
                    String frame = "fn:NoFrame";
                    for (int i = 0; i < mapping.size(); i++) {
                        String map = mapping.get(i);
                        if (map.startsWith("fn:")) {
                            frame = map;
                            break;
                        }
                    }
                    if (frameMaps.containsKey(frame)) {
                        ArrayList<String> frameMappings = frameMaps.get(frame);
                        for (int i = 0; i < mapping.size(); i++) {
                            String s = mapping.get(i);
                            if (!frameMappings.contains(s)) {
                                frameMappings.add(s);
                            }
                        }
                        frameMaps.put(frame, frameMappings);
                    }
                    else {
                        frameMaps.put(frame,mapping);
                    }
                }
                Set framekeySet = frameMaps.keySet();
                Iterator frameKeys = framekeySet.iterator();
                while (frameKeys.hasNext()) {
                    String frame = (String) frameKeys.next();
                    ArrayList<String> frameMappings = frameMaps.get(frame);
                    if (LU) {
                      if (wordnetData.synsetToLexicalUnits.containsKey(key)) {
                          ArrayList<String> lus = wordnetData.synsetToLexicalUnits.get(key);
                          for (int u = 0; u < lus.size(); u++) {
                              String lu = lus.get(u);
                              matrixString = "";
                              if (wordnetData.lexicalUnitsToLemmas.containsKey(lu)) {
                                  String lemma = "lu-lemma" + ":" + wordnetData.lexicalUnitsToLemmas.get(lu);
                                  matrixString = lemma+" ";
                              }
                              matrixString += prefix+":"+lu;
                              str = matrixString;
                              for (int i = 0; i < frameMappings.size(); i++) {
                                  String s = frameMappings.get(i);
                                  str += " "+s;
                              }
                              str += "\n";
                              fos.write(str.getBytes());
                          }
                      }
                      else {

                          str = matrixString;
                          for (int i = 0; i < frameMappings.size(); i++) {
                              String s = frameMappings.get(i);
                              str += " " + s;
                          }
                          str += "\n";
                          fos.write(str.getBytes());
                      }
                    }
                    else {
                        str = matrixString;
                        for (int i = 0; i < frameMappings.size(); i++) {
                            String s = frameMappings.get(i);
                            str += " " + s;
                        }
                        str += "\n";
                        fos.write(str.getBytes());
                    }
                }
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    static void outputReducedMappingsSets (WordnetData wordnetData, HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMap, String filePath, String wordnetName, String prefix) {
        System.out.println("projectedPredicateMap = " + projectedPredicateMap.size());
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            Set keySet = projectedPredicateMap.keySet();
            Iterator keys = keySet.iterator();
            String str = "";
            while (keys.hasNext()) {
                String key = (String) keys.next();
                str = prefix+":"+key;
                String synsetString = wordnetData.getSynsetString(key);
                str += " "+wordnetName+"-synset:"+synsetString;
                ArrayList<ArrayList<String>> mappings = projectedPredicateMap.get(key);
                ArrayList<String> uniqueMaps = new ArrayList<String>();
                for (int m = 0; m < mappings.size(); m++) {
                    ArrayList<String> mapping =  mappings.get(m);
                    for (int i = 0; i < mapping.size(); i++) {
                        String map = mapping.get(i);
                        if (!uniqueMaps.contains(map)) {
                            uniqueMaps.add(map);
                        }
                    }
                }
                for (int i = 0; i < uniqueMaps.size(); i++) {
                    String map = uniqueMaps.get(i);
                    str += " "+map;
                }
                str += "\n";
                fos.write(str.getBytes());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    static HashMap<String, ArrayList<ArrayList<String>>> createHyperonymMappings (WordnetData wordnetData,
                                         HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapDirect,
                                         HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapNear,
                                         HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapOther) {
        HashMap<String, ArrayList<ArrayList<String>>> pmMappings = new HashMap<String, ArrayList<ArrayList<String>>>();
        Set keySet = wordnetData.synsetToEntries.keySet();
        Iterator keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (!projectedPredicateMapDirect.containsKey(key) &&
                !projectedPredicateMapNear.containsKey(key) &&
                !projectedPredicateMapOther.containsKey(key)) {
                ArrayList<String> parentChain = new ArrayList<String>();
                //System.out.println("key = " + key);
                wordnetData.getSingleHyperChain(key, parentChain);
                if (key.endsWith("d_n-31318-n")) {
                    System.out.println("hyper parentChain.toString() = " + parentChain.toString());
                }
                boolean match = false;
                for (int i = 0; i < parentChain.size(); i++) {
                    // break at the most specific level
                    String parentSynset = parentChain.get(i);
                    if (projectedPredicateMapDirect.containsKey(parentSynset)) {
                        ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapDirect.get(parentSynset);
                        pmMappings.put(key, pmMaps);
                        match = true;
                        break;
                    }
                    else if (projectedPredicateMapNear.containsKey(parentSynset)) {
                        ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapNear.get(parentSynset);
                        pmMappings.put(key, pmMaps);
                        match = true;
                        break;
                    }
                    else if (projectedPredicateMapOther.containsKey(parentSynset)) {
                        ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapOther.get(parentSynset);
                        pmMappings.put(key, pmMaps);
                        match = true;
                        break;
                    }
                }
                /*if (!match) {
                    parentChain = new ArrayList<String>();
                    wordnetData.getXposRelationChain(key, parentChain);
                    if (key.endsWith("d_n-31318-n")) {
                        System.out.println("other parentChain.toString() = " + parentChain.toString());
                    }
                    for (int i = 0; i < parentChain.size(); i++) {
                        // break at the most specific level
                        String parentSynset = parentChain.get(i);
                        if (projectedPredicateMapDirect.containsKey(parentSynset)) {
                            ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapDirect.get(parentSynset);
                            pmMappings.put(key, pmMaps);
                            match = true;
                            break;
                        }
                        else if (projectedPredicateMapNear.containsKey(parentSynset)) {
                            ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapNear.get(parentSynset);
                            pmMappings.put(key, pmMaps);
                            match = true;
                            break;
                        }
                        else if (projectedPredicateMapOther.containsKey(parentSynset)) {
                            ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapOther.get(parentSynset);
                            pmMappings.put(key, pmMaps);
                            match = true;
                            break;
                        }
                    }
                }*/
            }
        }
        return pmMappings;
    }

    static HashMap<String, ArrayList<ArrayList<String>>> createDirectHyperonymMappings (WordnetData wordnetData,
                                         HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapDirect,
                                         HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapNear,
                                         HashMap<String, ArrayList<ArrayList<String>>> projectedPredicateMapOther) {
        HashMap<String, ArrayList<ArrayList<String>>> pmMappings = new HashMap<String, ArrayList<ArrayList<String>>>();
        Set keySet = wordnetData.synsetToEntries.keySet();
        Iterator keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (!projectedPredicateMapDirect.containsKey(key) &&
                !projectedPredicateMapNear.containsKey(key) &&
                !projectedPredicateMapOther.containsKey(key)) {
                ArrayList<String> parentChain = new ArrayList<String>();
                //System.out.println("key = " + key);
                wordnetData.getSingleHyperChain(key, parentChain);
                if (key.endsWith("d_n-31318-n")) {
                    System.out.println("hyper parentChain.toString() = " + parentChain.toString());
                }
                boolean match = false;
                for (int i = 0; i == 1; i++) {
                    // break at the most specific level
                    String parentSynset = parentChain.get(i);
                    if (projectedPredicateMapDirect.containsKey(parentSynset)) {
                        ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapDirect.get(parentSynset);
                        pmMappings.put(key, pmMaps);
                        match = true;
                        break;
                    }
                    else if (projectedPredicateMapNear.containsKey(parentSynset)) {
                        ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapNear.get(parentSynset);
                        pmMappings.put(key, pmMaps);
                        match = true;
                        break;
                    }
                    else if (projectedPredicateMapOther.containsKey(parentSynset)) {
                        ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapOther.get(parentSynset);
                        pmMappings.put(key, pmMaps);
                        match = true;
                        break;
                    }
                }
/*
                if (!match) {
                    parentChain = new ArrayList<String>();
                    wordnetData.getXposRelationChain(key, parentChain);
                    if (key.endsWith("d_n-31318-n")) {
                        System.out.println("other parentChain.toString() = " + parentChain.toString());
                    }
                    for (int i = 0; i < parentChain.size(); i++) {
                        // break at the most specific level
                        String parentSynset = parentChain.get(i);
                        if (projectedPredicateMapDirect.containsKey(parentSynset)) {
                            ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapDirect.get(parentSynset);
                            pmMappings.put(key, pmMaps);
                            match = true;
                            break;
                        }
                        else if (projectedPredicateMapNear.containsKey(parentSynset)) {
                            ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapNear.get(parentSynset);
                            pmMappings.put(key, pmMaps);
                            match = true;
                            break;
                        }
                        else if (projectedPredicateMapOther.containsKey(parentSynset)) {
                            ArrayList<ArrayList<String>> pmMaps = projectedPredicateMapOther.get(parentSynset);
                            pmMappings.put(key, pmMaps);
                            match = true;
                            break;
                        }
                    }
                }
*/
            }
        }
        return pmMappings;
    }

    static HashMap<String, ArrayList<ArrayList<String>>> createMapping (HashMap<String, ArrayList<String>> equivalenceMappings) {
        HashMap<String, ArrayList<ArrayList<String>>> pmMappings = new HashMap<String, ArrayList<ArrayList<String>>>();
        Set keySet = equivalenceMappings.keySet();
        Iterator keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            //System.out.println("key = " + key);
            ArrayList<String> equivalences = equivalenceMappings.get(key);
            for (int i = 0; i < equivalences.size(); i++) {
                String target = equivalences.get(i).toLowerCase();
                if (key.endsWith("d_n-31318-n"))   {
                  //  System.out.println("target = " + target);
                }
                if (wordNetPredicateMap.containsKey(target)) {
                    ArrayList<ArrayList<String>> mappings = wordNetPredicateMap.get(target);
                    if (pmMappings.containsKey(key)) {
/*                        ArrayList<ArrayList<String>> pmMaps = pmMappings.get(key);
                        for (int j = 0; j < mappings.size(); j++) {
                            ArrayList<String> strings = mappings.get(j);
                            pmMaps.add(strings);
                            pmMappings.put(key, pmMaps);
                        }*/
                    }
                    else {
                        pmMappings.put(key, mappings);
                    }
                }
                else {
                  //  System.out.println("Could not find the target = " + target);
                }
            }
        }
        return pmMappings;
    }

    static String getILI(String [] fields) {
        String ili = "";
        for (int i = 0; i < fields.length; i++) {
            String header = fields[i];
            if (header.startsWith("mcr:ili")) {
                ili = "eng" + header.substring(7);  //mcr:ili-30-00619869-v    eng-30-00619869-v
                //System.out.println("ili = " + ili);
                break;
            }
        }
        return ili;
    }

    public static void processMatrixFileWithWordnetSynset(String file) {
        try {
           /*
           VN_CLASS VN_CLASS_NUMBER VN_SUBCLASS VN_SUBCLASS_NUMBER VN_LEMA WN_SENSE VN_THEMROLE FN_FRAME FN_LEXENT FN_ROLE PB_ROLESET PB_ARG MCR_ILIOFFSET MCR_DOMAIN MCR_SUMO MC_LEXNAME
vn:comprehend-87.2 vn:87.2 vn:null vn:null vn:misconstrue wn:misconstrue%2:31:01 vn:Experiencer fn:NULL fn:NULL fn:NULL pb:misconstrue.01 pb:0 mcr:ili-30-00619869-v mcr:factotum mcr:Communication mcr:cognition
vn:comprehend-87.2 vn:87.2 vn:null vn:null vn:misconstrue wn:misconstrue%2:31:01 vn:Attribute fn:NULL fn:NULL fn:NULL pb:misconstrue.01 pb:1 mcr:ili-30-00619869-v mcr:factotum mcr:Communication mcr:cognition
vn:comprehend-87.2 vn:87.2 vn:null vn:null vn:misconstrue wn:misconstrue%2:31:01 vn:Stimulus fn:NULL fn:NULL fn:NULL pb:misconstrue.01 pb:NULL mcr:ili-30-00619869-v mcr:factotum mcr:Communication mcr:cognition
vn:comprehend-87.2 vn:87.2 vn:null vn:null vn:misinterpret wn:misinterpret%2:31:02 vn:Experiencer fn:NULL fn:NULL fn:NULL pb:misinterpret.01 pb:0 mcr:ili-30-00619869-v mcr:factotum mcr:Communication mcr:cognition
vn:comprehend-87.2 vn:87.2 vn:null vn:null vn:misinterpret wn:misinterpret%2:31:02 vn:Attribute fn:NULL fn:NULL fn:NULL pb:misinterpret.01 pb:2 mcr:ili-30-00619869-v mcr:factotum mcr:Communication mcr:cognition

1_VN_CLASS      2_VN_CLASS_NUMBER       3_VN_SUBCLASS   4_VN_SUBCLASS_NUMBER    5_VN_LEMA       6_VN_ROLE       7_WN_SENSE      8_MCR_iliOffset 9_FN_FRAME      10_FN_LE        11_FN_FRAME_ELEMENT     12_PB_ROLESET   13_PB_ARG       14_MCR_BC       15_MCR_DOMAIN   16_MCR_SUMO     17_MCR_TO       18_MCR_LEXNAME  19_MCR_BLC      20_WN_SENSEFREC 21_WN_SYNSET_REL_NUM    22_SOURC
vn:accept-77	vn:77	vn:NULL	vn:NULL	vn:accept	vn:Agent	wn:accept%2:31:01	mcr:ili-30-00719231-v	fn:Receiving	fn:NULL	fn:Theme	pb:accept.01	pb:0	mcr:0	mcr:factotum	mcr:IntentionalPsychologicalProcess	mcr:Agentive;Dynamic;	mcr:cognition	mcr:act%2:41:00	wn:12	wn:009	SEMLINK;FN_FE
vn:accept-77	vn:77	vn:NULL	vn:NULL	vn:accept	vn:Agent	wn:accept%2:31:01	mcr:ili-30-00719231-v	fn:Respond_to_proposal	fn:NULL	fn:Speaker	pb:accept.01	pb:0	mcr:0	mcr:factotum	mcr:IntentionalPsychologicalProcess	mcr:Agentive;Dynamic;	mcr:cognition	mcr:act%2:41:00	wn:12	wn:009	SEMLINK;FN_FE

            */
            String [] headers = null;
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader in = new BufferedReader(isr);
            String inputLine = "";
            String synset = "";
            String senseKey ="";
            String lemma = "";
            if (in.ready()&&(inputLine = in.readLine()) != null) {
                /// processing the header
                //String headerString = inputLine.replaceAll("\t", "  ");
/*              /// Does not work because the headers are separated by inconsistent spaces
                String headerString = inputLine;
                headers = headerString.split("   ");
                System.out.println("headers.length = " + headers.length);
*/
            }
            while (in.ready()&&(inputLine = in.readLine()) != null) {
                if (inputLine.trim().length()>0) {
                    String[] fields = inputLine.split("\t");
                   // System.out.println("fields = " + fields.length);
                    if (fields.length==1) {
                        fields = inputLine.split(" ");
                    }
                   // System.out.println("fields = " + fields.length);
                    synset = getILI(fields);
                    if (synset.isEmpty()) {
                        continue;
                    }
                    ArrayList<String> roleFields = new ArrayList<String>();
                    ArrayList<String> sourceFields = new ArrayList<String>();
                    for (int i = 0; i < fields.length; i++) {
                        String field = fields[i].trim();
                        if (!field.isEmpty() && (field.toLowerCase().indexOf("null")==-1)) {
                            if (field.indexOf("-role:")>-1) {
                                roleFields.add(field);
                            }
                            else {
                                sourceFields.add(field);
                            }
                        }
                    }
                    for (int i = 0; i < roleFields.size(); i++) {
                        String s = roleFields.get(i);
                        if (s.startsWith("fn-role:")) {
                            boolean paired = false;
                            for (int j = 0; j < roleFields.size(); j++) {
                                if (j!=i) {
                                    String s1 = roleFields.get(j);
                                    if (s1.startsWith("pb-role:")) {
                                        paired = true;
                                        String role = "fn-pb-role:"+s.substring(8)+"#"+s1.substring(8);
                                        sourceFields.add(role);
                                    }
                                }

                            }
                            if (!paired) {
                                sourceFields.add(s);
                            }
                        }
                    }
                    if (sourceFields.size()>0) {
                        if (wordNetPredicateMap.containsKey(synset)) {
                            ArrayList<ArrayList<String>> targets = wordNetPredicateMap.get(synset);

                            if (!hasSourceField(targets, sourceFields)) {
                                targets.add(sourceFields);
                                wordNetPredicateMap.put(synset, targets);

                            }

                        }
                        else {
                            ArrayList<ArrayList<String>> targets = new ArrayList<ArrayList<String>>();
                            targets.add(sourceFields);
                            wordNetPredicateMap.put(synset, targets);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    static boolean hasSourceField (ArrayList<ArrayList<String>> targets, ArrayList<String> sourceField) {
        for (int i = 0; i < targets.size(); i++) {
            ArrayList<String> strings = targets.get(i);
            if (strings.containsAll(sourceField)) return true;
        }
        return false;
    }
}
