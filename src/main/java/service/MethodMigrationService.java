package service;

import api.entity.MigrationMethodUnit;
import api.service.MigrationChangeSet;
import api.service.MigrationPackage;
import api.service.MigrationService;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import impl.MigrationChangeSetImpl;
import impl.matcher.MigrationUnitWithClassMatcher;
import impl.matcher.MigrationUnitWithMethodMatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodMigrationService implements MigrationService {

    private List<MigrationMethodUnit> units = Collections.emptyList();

    private MigrationUnitWithClassMatcher<NodeWithName, MigrationMethodUnit> classMatcher = new MigrationUnitWithClassMatcher<>();
    private MigrationUnitWithMethodMatcher<NodeWithSimpleName, MigrationMethodUnit> methodMatcher = new MigrationUnitWithMethodMatcher<>();

    @Override
    public MigrationService setup(MigrationPackage mu) {
        units = mu.getMethods();
        return this;
    }

    @Override
    public MigrationChangeSet migrate(CompilationUnit cu) {
        Map<Node, Node> changeSet = new HashMap<>();

        cu.findAll(MethodCallExpr.class, n -> methodMatcher.matchesMethod(n, units) && hasImport(cu, units))
                .forEach(n -> methodMatcher.findByMethod(n, units)
                        .ifPresent(u -> {
                            Expression scope = null;
                            if(n.getScope().isPresent()) {
                                scope = new NameExpr(u.getNewIdentifier());
                            }
                            MethodCallExpr expr = new MethodCallExpr(scope, u.getNewMethod(), n.getArguments());
                            changeSet.put(n, expr);
                        }));

        return new MigrationChangeSetImpl(changeSet);
    }

    private boolean hasImport(CompilationUnit cu, List<MigrationMethodUnit> units) {
        return !cu.findAll(ImportDeclaration.class, n -> classMatcher.matchesQualifier(n, units) && n.isAsterisk()).isEmpty()
                || !cu.findAll(ImportDeclaration.class, n -> classMatcher.matchesName(n, units)).isEmpty()
                || !cu.findAll(ImportDeclaration.class, n -> methodMatcher.matchesStaticMethod(n, units) && n.isStatic());
    }
}