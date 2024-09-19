package edu.berkeley.cs.jqf.examples.chocopy;

import chocopy.common.analysis.types.FuncType;
import chocopy.common.analysis.types.Type;
import com.pholser.junit.quickcheck.Pair;

import java.sql.Array;
import java.util.*;

public class Scope {

    public String name;
    private Scope parentScope;
    private List<Scope> childScopes;
    public Map<String, Type> varTypes;
    public Map<String, FuncType> funcTypes;

    public Scope(String name, Scope parentScope) {
        this.name = name;
        this.parentScope = parentScope;
        this.childScopes = new ArrayList<>();
        this.varTypes = new HashMap<>();
        this.funcTypes = new HashMap<>();
    }

    public Scope getParent() {
        return parentScope;
    }

    public void addChild(Scope child) {
        childScopes.add(child);
    }

    public List<Pair<String, Type>> getNonlocalVars(Scope globalScope) {
        List<Pair<String, Type>> nonlocalVars = new ArrayList<>();
        if (parentScope == null || parentScope.name.equals("global")) {
            return nonlocalVars;
        }
        for (Map.Entry<String, Type> entry : parentScope.varTypes.entrySet()) {
            String varName = entry.getKey();
            if (varTypes.containsKey(varName) || globalScope.varTypes.containsKey(varName)) {
                continue;
            }
            nonlocalVars.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        return nonlocalVars;
    }

    public List<String> getVarsOfType(Type type, boolean onlyCurrentScope) {
        List<String> varTypesList = new ArrayList<>();
        for (Map.Entry<String, Type> entry : varTypes.entrySet()) {
            if (entry.getValue().equals(type)) {
                varTypesList.add(entry.getKey());
            }
        }
        if (onlyCurrentScope || parentScope == null) {
            return varTypesList;
        }
        varTypesList.addAll(parentScope.getVarsOfType(type, false));
        return varTypesList;
    }

    public List<Pair<String, FuncType>> getFuncsWithReturnType(Type type) {
        List<Pair<String, FuncType>> funcTypesList = new ArrayList<>();
        for (Map.Entry<String, FuncType> entry : funcTypes.entrySet()) {
            FuncType funcType = entry.getValue();
            if (funcType.returnType.equals(type)) {
                funcTypesList.add(new Pair(entry.getKey(), funcType));
            }
        }
        if (parentScope == null) {
            return funcTypesList;
        }
        funcTypesList.addAll(parentScope.getFuncsWithReturnType(type));
        return funcTypesList;
    }

    public List<Pair<String, FuncType>> getMethodsWithReturnType(Type type) {
        List<Pair<String, FuncType>> funcTypesList = new ArrayList<>();
        for (Map.Entry<String, FuncType> entry : funcTypes.entrySet()) {
            String funcName = entry.getKey();
            if (!funcName.contains(".")) {
                continue;
            }
            FuncType funcType = entry.getValue();
            if (funcType.returnType.equals(type)) {
                funcTypesList.add(new Pair<>(entry.getKey(), funcType));
            }
        }
        if (parentScope == null) {
            return funcTypesList;
        }
        funcTypesList.addAll(parentScope.getFuncsWithReturnType(type));
        return funcTypesList;
    }
}
