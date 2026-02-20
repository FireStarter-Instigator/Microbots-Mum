import tkinter as tk
from tkinter import messagebox, scrolledtext
import subprocess
import threading
import os
import shutil
import time
import re
from datetime import datetime

# =============================================================================
# === CONFIGURATION: ACTION REQUIRED HERE ===
# =============================================================================

# 1. Your NVMe Drive Letter (e.g., "D", "E")
NVME_DRIVE_LETTER = "H"

# 2. FULL PATH TO MAVEN EXECUTABLE (REQUIRED)
MAVEN_EXECUTABLE = r"C:\Program Files\apache-maven-3.9.11\bin\mvn.cmd" 

# Max Retries for a failed build
MAX_RETRIES = 3

# === ARTIFACT FILTER BY SIZE ===
# 55 MB filter in bytes
MIN_ARTIFACT_SIZE_BYTES = 57671680 

# =============================================================================

class NvmeAutomationEmpire:
    def __init__(self, root):
        self.root = root
        self.root.title("Multi-Build Manager: Auto-Version POM Updater")
        self.root.geometry("750x700")
        
        self.source_path = os.getcwd()
        
        # --- READABLE PATH GENERATION ---
        current_date = datetime.now().strftime("%Y-%m-%d")
        current_time = datetime.now().strftime("%H-%M-%S")
        
        self.nvme_root = f"{NVME_DRIVE_LETTER}:\\Automation_Empire"
        
        # Path where Script 1 saves the version number
        self.version_file_path = os.path.join(self.nvme_root, "target_version.txt")

        self.backup_dest_parent = os.path.join(self.nvme_root, "Backups", current_date)
        self.backup_dest = os.path.join(self.backup_dest_parent, f"Snap_{current_time}")
        
        self.artifacts_dest_parent = os.path.join(self.nvme_root, "Builds", current_date)
        self.artifacts_dest = os.path.join(self.artifacts_dest_parent, f"Builds_{current_time}")
        # ------------------------------------

        # --- UI HEADER ---
        tk.Label(root, text="NVMe Build Commander", font=("Segoe UI", 16, "bold")).pack(pady=(15, 5))
        
        info_frame = tk.Frame(root)
        info_frame.pack(pady=5)
        tk.Label(info_frame, text=f"Source: {self.source_path}", fg="gray").pack()
        tk.Label(info_frame, text=f"Target: {self.backup_dest_parent}", fg="blue").pack()

        # --- OPTIONS ---
        opts_frame = tk.Frame(root)
        opts_frame.pack(pady=5)
        
        # Default to True so tests are skipped by default
        self.var_skip_tests = tk.BooleanVar(value=True)
        tk.Checkbutton(opts_frame, text="Skip Tests (-DskipTests)", variable=self.var_skip_tests).pack(side="left", padx=10)

        # --- BRANCH LIST ---
        tk.Label(root, text="Select Branches:", font=("Segoe UI", 10, "bold")).pack(anchor="w", padx=20, pady=(10,0))

        self.canvas = tk.Canvas(root, borderwidth=0, background="#ffffff")
        self.frame = tk.Frame(self.canvas, background="#ffffff")
        self.vsb = tk.Scrollbar(root, orient="vertical", command=self.canvas.yview)
        self.canvas.configure(yscrollcommand=self.vsb.set)

        self.vsb.pack(side="right", fill="y")
        self.canvas.pack(side="top", fill="both", expand=True, padx=20, pady=5)
        self.canvas.create_window((4,4), window=self.frame, anchor="nw")

        self.frame.bind("<Configure>", lambda event, canvas=self.canvas: canvas.configure(scrollregion=canvas.bbox("all")))

        # Load Branches
        self.checkbox_vars = {}
        branches = self.get_git_branches()
        for branch in branches:
            var = tk.BooleanVar()
            chk = tk.Checkbutton(self.frame, text=branch, variable=var, bg="#ffffff", anchor='w')
            chk.pack(fill='x', expand=True)
            self.checkbox_vars[branch] = var

        # --- CONTROLS ---
        btn_frame = tk.Frame(root)
        btn_frame.pack(pady=10)
        
        tk.Button(btn_frame, text="Select All", command=self.select_all).pack(side="left", padx=10)
        self.btn_run = tk.Button(btn_frame, text="ENGAGE AUTOMATION", bg="#007acc", fg="white", font=("Segoe UI", 10, "bold"), command=self.start_thread)
        self.btn_run.pack(side="left", padx=10)

        # --- LOG ---
        self.log_text = scrolledtext.ScrolledText(root, height=15, state='disabled', bg="#2b2b2b", fg="#00ff00", font=("Consolas", 9))
        self.log_text.pack(fill="x", padx=20, pady=(0, 20))

    def log(self, message):
        self.log_text.config(state='normal')
        self.log_text.insert(tk.END, f"[{datetime.now().strftime('%H:%M:%S')}] {message}\n")
        self.log_text.see(tk.END)
        self.log_text.config(state='disabled')

    def get_git_branches(self):
        try:
            res = subprocess.run(["git", "branch", "--format=%(refname:short)"], capture_output=True, text=True)
            return [b.strip() for b in res.stdout.split('\n') if b.strip()]
        except:
            return ["Error: Not a git repo"]

    def select_all(self):
        for var in self.checkbox_vars.values():
            var.set(True)

    def start_thread(self):
        threading.Thread(target=self.run_process).start()

    # === NEW: HELPER TO UPDATE POMS ===
    def get_target_version(self):
        """Reads the version file saved by Script 1"""
        if os.path.exists(self.version_file_path):
            with open(self.version_file_path, "r") as f:
                return f.read().strip()
        return None

    def update_pom_versions(self, repo_path, new_version):
        """Walks the directory and updates pom.xml files to the new version"""
        count = 0
        self.log(f"    Scanning for POMs in: {repo_path}")
        
        for root, dirs, files in os.walk(repo_path):
            if "pom.xml" in files:
                pom_path = os.path.join(root, "pom.xml")
                
                # Check if this is the sensitive "runelite-client" folder
                # os.sep handles both Windows (\) and Linux (/) separators
                is_client_pom = f"runelite-client{os.sep}pom.xml" in pom_path or "runelite-client/pom.xml" in pom_path.replace("\\", "/")

                with open(pom_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content

                # Logic 1: Update 'injected-client' dependency (CRITICAL)
                # Matches <artifactId>injected-client</artifactId> ... <version>X.X.X</version>
                injected_regex = r'(?s)(<artifactId>injected-client</artifactId>.*?<version>)([\d\.]+)(</version>)'
                if re.search(injected_regex, content):
                    content = re.sub(injected_regex, fr'\g<1>{new_version}\g<3>', content)
                    count += 1
                
                # Logic 2: Update <runelite.version> property (If used)
                prop_regex = r'(<runelite.version>)([\d\.]+)(</runelite.version>)'
                if re.search(prop_regex, content):
                    content = re.sub(prop_regex, fr'\g<1>{new_version}\g<3>', content)
                    count += 1

                # Logic 3: Update Standard 1.12.X Version Tags
                # This Regex STRICTLY matches numbers (1.12.X).
                # It catches <parent> tags but IGNORES custom text names like "InFiNiTy"
                project_ver_regex = r'(?s)(<version>)(1\.12\.[\d]+(?:-SNAPSHOT)?)(</version>)'
                if re.search(project_ver_regex, content):
                    content = re.sub(project_ver_regex, fr'\g<1>{new_version}\g<3>', content)
                    count += 1

                if content != original_content:
                    with open(pom_path, 'w', encoding='utf-8') as f:
                        f.write(content)
                    self.log(f"      - Patched {os.path.basename(root)}/pom.xml")
        
        return count

    def run_process(self):
        selected = [b for b, var in self.checkbox_vars.items() if var.get()]
        if not selected:
            messagebox.showwarning("Wait", "No branches selected!")
            return

        self.btn_run.config(state="disabled", text="Processing...")
        
        # 0. Check for Target Version
        target_version = self.get_target_version()
        if target_version:
            self.log(f"Phase 0: Target Version Detected: {target_version}")
        else:
            self.log("Phase 0: No target_version.txt found. Skipping auto-update of POMs.")

        # 1. Setup Folders
        try:
            if not os.path.exists(self.artifacts_dest): os.makedirs(self.artifacts_dest)
            if not os.path.exists(self.backup_dest): os.makedirs(self.backup_dest)
        except Exception as e:
            self.log(f"Error creating folders: {e}")
            return

        # 2. Clone
        self.log("Phase 1: Cloning to NVMe...")
        self.log(f"Target Backup Folder: {self.backup_dest}")
        
        clone_res = subprocess.run(["git", "clone", self.source_path, self.backup_dest], capture_output=True, text=True)
        
        if clone_res.returncode != 0:
            self.log("CRITICAL: Clone Failed.")
            self.log(f"GIT ERROR: {clone_res.stderr}") 
            self.btn_run.config(state="normal", text="ENGAGE AUTOMATION")
            return
        self.log("Clone Complete.")

        # DIAGNOSTIC: Check if pom.xml exists
        pom_path = os.path.join(self.backup_dest, "pom.xml")
        if not os.path.exists(pom_path):
            self.log("CRITICAL ERROR: pom.xml not found in backup folder!")
            self.log(f"Looking for: {pom_path}")
            self.btn_run.config(state="normal", text="ENGAGE AUTOMATION")
            return

        # 3. Build Loop with Retry
        base_mvn = f'"{MAVEN_EXECUTABLE}" clean install'
        
        # Use stronger test skipping flags if checked
        if self.var_skip_tests.get():
            base_mvn += " -DskipTests -Dmaven.test.skip=true -Dmaven.javadoc.skip=true"

        self.log(f"Phase 2: Building {len(selected)} branches...")
        
        for branch in selected:
            self.log(f"--> Target: {branch}")
            
            # Checkout
            checkout_res = subprocess.run(["git", "checkout", branch], cwd=self.backup_dest, capture_output=True, text=True)
            if checkout_res.returncode != 0:
                self.log(f"    Could not switch branch: {checkout_res.stderr}")
                continue
            
            # === AUTO UPDATE POMS FOR THIS BRANCH ===
            if target_version:
                updates = self.update_pom_versions(self.backup_dest, target_version)
                self.log(f"    Auto-updated {updates} POM references to {target_version}")

            # === MEMORY FIX: CREATE CUSTOM ENV ===
            build_env = os.environ.copy()
            build_env["MAVEN_OPTS"] = "-Xmx4096m" 

            # RETRY LOOP
            success = False
            for attempt in range(1, MAX_RETRIES + 1):
                # Run Build
                build_res = subprocess.run(base_mvn, cwd=self.backup_dest, shell=True, capture_output=True, text=True, env=build_env)
                
                if build_res.returncode == 0:
                    success = True
                    self.log(f"    SUCCESS.")
                    break
                else:
                    if attempt == 1:
                        err_msg = build_res.stderr
                        if not err_msg: err_msg = build_res.stdout
                        
                        self.log(f"    FAIL REASON (Snippet): {err_msg[-300:]}")
                        if "OutOfMemoryError" in err_msg:
                             self.log("    !! RAM ERROR DETECTED (Even with 4GB). Retrying...")
                        if "'mvn' is not recognized" in err_msg or "command not found" in err_msg:
                            self.log("    !! ACTION REQUIRED: Python cannot find 'mvn'. Edit MAVEN_EXECUTABLE in the script.")
                    
                    if attempt < MAX_RETRIES:
                        time.sleep(2) 
            
            if success:
                # Move Artifact - FIND LARGEST JAR OVER 55MB
                target_root = os.path.join(self.backup_dest) 
                found_artifact = None
                max_size = 0
                
                for root_dir, _, files in os.walk(target_root):
                    if 'target' in root_dir and 'classes' not in root_dir and 'test-classes' not in root_dir:
                        for f in files:
                            f_lower = f.lower()
                            
                            if f_lower.endswith(".jar") or f_lower.endswith(".war"):
                                artifact_path = os.path.join(root_dir, f)
                                current_size = os.path.getsize(artifact_path)
                                
                                if current_size >= MIN_ARTIFACT_SIZE_BYTES and current_size > max_size:
                                    max_size = current_size
                                    found_artifact = artifact_path
                
                if found_artifact:
                    artifact_filename = os.path.basename(found_artifact)
                    new_name = artifact_filename 
                    
                    shutil.copy2(found_artifact, os.path.join(self.artifacts_dest, new_name))
                    self.log(f"    Artifact copied: {new_name} ({round(max_size / 1048576, 2)} MB)")
                else:
                    self.log(f"    Warning: Build passed, but no JAR/WAR artifact found.")
            else:
                self.log(f"    ERROR: {branch} failed.")
            
            # Reset all changes (including POM modifications) before switching to next branch
            self.log(f"    Resetting changes for next branch...")
            subprocess.run(["git", "reset", "--hard"], cwd=self.backup_dest, capture_output=True)
            subprocess.run(["git", "clean", "-fd"], cwd=self.backup_dest, capture_output=True)

        self.log("Phase 3: Operations Complete.")
        self.btn_run.config(state="normal", text="ENGAGE AUTOMATION")
        messagebox.showinfo("Empire Notification", f"Process Complete.\n\nSaved to:\n{self.artifacts_dest}")

if __name__ == "__main__":
    root = tk.Tk()
    app = NvmeAutomationEmpire(root)
    root.mainloop()