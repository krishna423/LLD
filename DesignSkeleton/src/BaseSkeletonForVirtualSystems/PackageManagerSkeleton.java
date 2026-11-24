package BaseSkeletonForVirtualSystems;
import java.time.LocalDateTime;
import java.util.*;

public class PackageManagerSkeleton {

// ================= ENUMS =================

    enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

// ================= REGISTRY INTERFACES =================

    interface PackageRegistry {
        void registerPackage(RegistryPackage pkg);
        void publishVersion(String packageName, PackageVersion version);
        RegistryPackage getPackage(String packageName);
        Collection<RegistryPackage> getAllPackages();

        //Map<String, RegistryPackage> packages = new HashMap<>();
    }

    interface AdvisoryRegistry {
        void publish(SecurityAdvisory advisory);
        Collection<SecurityAdvisory> getAllAdvisories();
    }

    interface IDependencyGraphManager {
        // Dependency -> Parent package, those depend on it
        // Map<String, Set<String>> dependentsGraph = new HashMap<>();
        // springCore:6.1.1 --> [springBoot:3.6.1,springBoot:3.6.3]

        //1.buildDependencyMapUsingSchedular()
        //Pick one-by-one package , explore dependency and create map use queue to create map

        //2.Update this mapping on each new version registration
        void addDependentsMapping(String parent, List<Dependency> dependencyList);
    }

    interface IPackageAnalyzer{
        VulnerabilityFinding scanPackages(String vulnerableNodeWithVersion) ;
        //Use dependencyMap to get this details
    }


    interface VersionMatcher {
        boolean isAffected(String packageVersion, SecurityAdvisory advisory);
    }


// ================= DOMAIN MODELS =================

    static class Dependency {
          String packageName;
          String resolvedVersion;
    }

    static class PackageVersion {
        String version;
        List<Dependency> dependencies;
    }

    static class RegistryPackage {
        String name;
        String ownerName;
        Map<String, PackageVersion> versions = new HashMap<>();
    }

    static class SecurityAdvisory {
        String advisoryId;
        String packageName;
        String affectedVersion;
        Severity severity;
        LocalDateTime publishedAt;
    }

    static class VulnerabilityFinding {
        String advisoryId;
        String vulnerablePackage;
        String vulnerableVersion;
        Severity severity;
        Set<String> affectedPackages;
    }
































// ================= DEPENDENCY GRAPH =================

    static class DependencyGraph implements IDependencyGraphManager {

        // Dependency -> Packages that depend on it
        private final Map<String, Set<String>> dependentsGraphMap = new HashMap<>();


        public Map<String, Set<String>> getDependentsGraphMap(){ return dependentsGraphMap;}

        public void addDependentsMapping(String parent, List<Dependency> dependencyList) {
            for (Dependency dependency : dependencyList) {
                String dependencyFinalName = dependency.packageName + ":" + dependency.resolvedVersion;
                dependentsGraphMap.computeIfAbsent(dependencyFinalName, k -> new HashSet<>()).add(parent);
            }
        }
    }

    static class PackageAnalyzer implements IPackageAnalyzer{

        // Dependency -> Packages that depend on it
        DependencyGraph dependencyGraph;
         public VulnerabilityFinding scanPackages(String vulnerableNode) {

             VulnerabilityFinding vulnerabilityFinding = new VulnerabilityFinding();

            Set<String> result = new HashSet<>();
            Queue<String> queue = new LinkedList<>();
            Set<String> visited = new HashSet<>();

            //search all version of that package
            //Use Matching strategy to match the version and add in queue
            queue.add(vulnerableNode);
            visited.add(vulnerableNode);

            while (!queue.isEmpty()) {

                String current = queue.poll();

                for (String dependent : dependencyGraph.dependentsGraphMap.getOrDefault(current, Collections.emptySet())) {

                    if (visited.add(dependent)) {
                        result.add(dependent);
                        queue.add(dependent);
                    }
                }
            }
            vulnerabilityFinding.affectedPackages= result;
            return vulnerabilityFinding;
        }
    }



    // Simplified exact-version matching for this interview implementation
    static class ExactVersionMatcher implements VersionMatcher {

        @Override
        public boolean isAffected(String packageVersion, SecurityAdvisory advisory) {
            return packageVersion.equals(advisory.affectedVersion);
        }
    }

    // ================= IN-MEMORY REGISTRIES =================

    static class InMemoryPackageRegistry implements PackageRegistry {

        private final Map<String, RegistryPackage> packages = new HashMap<>();

        @Override
        public void registerPackage(RegistryPackage pkg) {
            packages.put(pkg.name, pkg);
        }

        @Override
        public void publishVersion(String packageName, PackageVersion packageVersion) {
            RegistryPackage pkg = packages.get(packageName);
            pkg.versions.put(packageVersion.version, packageVersion);
        }

        @Override
        public RegistryPackage getPackage(String packageName) {
            return packages.get(packageName);
        }

        @Override
        public Collection<RegistryPackage> getAllPackages() {
            return packages.values();
        }
    }

    static class InMemoryAdvisoryRegistry implements AdvisoryRegistry {

        private final Map<String, SecurityAdvisory> advisories = new HashMap<>();

        @Override
        public void publish(SecurityAdvisory advisory) {
            advisories.put(advisory.advisoryId, advisory);
        }

        @Override
        public Collection<SecurityAdvisory> getAllAdvisories() {
            return advisories.values();
        }
    }

}
