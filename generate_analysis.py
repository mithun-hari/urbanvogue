import os
import re

def analyze_project(root_dir, output_file):
    services = ['auth-service', 'inventory-service', 'order-service', 'payment-service', 'product-service']
    
    with open(output_file, 'w', encoding='utf-8') as out:
        out.write("# UrbanVogue Comprehensive Project Analysis\n\n")
        out.write("This document provides an automated analysis of each microservice in the UrbanVogue e-commerce project.\n\n")
        
        for service in services:
            service_path = os.path.join(root_dir, service)
            if not os.path.exists(service_path):
                continue
                
            out.write(f"## {service.upper()}\n\n")
            
            # Analyze pom.xml
            pom_path = os.path.join(service_path, 'pom.xml')
            if os.path.exists(pom_path):
                with open(pom_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    deps = re.findall(r'<artifactId>(.*?)</artifactId>', content)
                    out.write("### Dependencies\n")
                    # Filter out purely internal/maven plugin noise if possible, or just list unique
                    deps = list(set([d for d in deps if 'maven' not in d and 'spring-boot-starter-test' not in d and 'lombok' not in d]))
                    for dep in sorted(deps):
                        if dep != service:
                            out.write(f"- `{dep}`\n")
                    out.write("\n")
            
            # Application properties/yml
            res_dir = os.path.join(service_path, 'src', 'main', 'resources')
            if os.path.exists(res_dir):
                out.write("### Configuration Files\n")
                for root_res, _, files in os.walk(res_dir):
                    for file in files:
                        if file.endswith('.yml') or file.endswith('.properties'):
                            out.write(f"- `{file}`\n")
                out.write("\n")
            
            # Analyze Java files
            java_src_dir = os.path.join(service_path, 'src', 'main', 'java')
            if os.path.exists(java_src_dir):
                out.write("### Components\n\n")
                
                controllers = []
                services_list = []
                repositories = []
                entities = []
                configs = []
                others = []
                
                for root_java, _, files in os.walk(java_src_dir):
                    for file in files:
                        if file.endswith('.java'):
                            filepath = os.path.join(root_java, file)
                            with open(filepath, 'r', encoding='utf-8') as f:
                                java_content = f.read()
                                class_match = re.search(r'(?:public\s+)?(?:class|interface|enum|record)\s+(\w+)', java_content)
                                if not class_match:
                                    continue
                                class_name = class_match.group(1)
                                
                                # Extract endpoints from controllers
                                endpoints = []
                                if '@RestController' in java_content or '@Controller' in java_content:
                                    mapping_matches = re.finditer(r'@(?:Get|Post|Put|Delete|Patch|Request)Mapping(?:\((.*?)\))?', java_content)
                                    for m in mapping_matches:
                                        mapping_val = m.group(1) if m.group(1) else '""'
                                        endpoints.append(mapping_val.strip().replace('"', '').replace('value =', '').strip())
                                    
                                    desc = f"- **{class_name}**"
                                    if endpoints:
                                        desc += f" (Endpoints: {', '.join(endpoints)})"
                                    controllers.append(desc)
                                elif '@Service' in java_content:
                                    services_list.append(f"- **{class_name}**")
                                elif '@Repository' in java_content or 'extends JpaRepository' in java_content:
                                    repositories.append(f"- **{class_name}**")
                                elif '@Entity' in java_content or '@Document' in java_content:
                                    entities.append(f"- **{class_name}**")
                                elif '@Configuration' in java_content:
                                    configs.append(f"- **{class_name}**")
                                else:
                                    others.append(f"- {class_name}")

                if controllers:
                    out.write("#### Controllers\n")
                    out.write('\n'.join(controllers) + "\n\n")
                if services_list:
                    out.write("#### Services\n")
                    out.write('\n'.join(services_list) + "\n\n")
                if repositories:
                    out.write("#### Repositories\n")
                    out.write('\n'.join(repositories) + "\n\n")
                if entities:
                    out.write("#### Entities/Models\n")
                    out.write('\n'.join(entities) + "\n\n")
                if configs:
                    out.write("#### Configurations\n")
                    out.write('\n'.join(configs) + "\n\n")
                if others:
                    out.write("#### Other Classes (DTOs, Exceptions, Utils)\n")
                    out.write(', '.join([o.replace("- ", "**") + "**" for o in others[:15]]))
                    if len(others) > 15:
                        out.write(f", and {len(others) - 15} more...")
                    out.write("\n\n")
                    
            out.write("---\n\n")

if __name__ == "__main__":
    root_directory = r"c:\Users\mithu\Desktop\urbanvogue"
    output_markdown = r"C:\Users\mithu\.gemini\antigravity\brain\33a772bd-b84f-4253-839f-888e904b70e4\project_analysis.md"
    analyze_project(root_directory, output_markdown)
    print("Analysis complete.")
