import subprocess
import tkinter as tk
import os
from tkinter import ttk, messagebox, filedialog

def run_rupee(search_mode, search_type, upload_path, output_path):
    script_path = "./run_rupee-search.sh"
    subprocess.run([script_path, search_mode, search_type, upload_path, output_path])
    messagebox.showinfo("Rupee", "Search has finished.")

def update_database():
    database_path = database_path_var.get()

    if not database_path:
        messagebox.showerror("Error", "Please, select a directory as the database.")
        return

    constants_file_path = "rupee-search/src/main/java/edu/umkc/rupee/search/lib/Constants.java"
    new_dir_path_line = f'    public final static String DIR_PATH = "{database_path}/";\n'

    current_dir = os.path.dirname(os.path.abspath(__file__))
    new_data_path_line = f'    public final static String DATA_PATH = "{current_dir}/data/";\n'

    with open(constants_file_path, "r") as file:
        lines = file.readlines()

    with open(constants_file_path, "w") as file:
        for line in lines:
            if line.strip().startswith("public final static String DIR_PATH"):
                file.write(new_dir_path_line)
            elif line.strip().startswith("public final static String DATA_PATH"):
                file.write(new_data_path_line)
            else:
                file.write(line)

    script_path = "./actualizar_bbdd.sh"
    subprocess.run([script_path])
    messagebox.showinfo("Rupee", "Database has been updated.")

def submit_form():
    search_mode = search_mode_var.get()
    search_type = search_type_var.get()
    upload_path = upload_path_var.get()
    output_path = output_path_var.get()

    if not search_mode or not search_type or not upload_path or not output_path:
        messagebox.showerror("Error", "Please, fill out all fields.")
        return

    run_rupee(search_mode, search_type, upload_path, output_path)

def browse_directory():
    directory = filedialog.askdirectory()
    database_path_var.set(directory)

def browse_upload_directory():
    directory = filedialog.askdirectory()
    upload_path_var.set(directory)

def browse_output_directory():
    directory = filedialog.askdirectory()
    output_path_var.set(directory)

app = tk.Tk()
app.title("RUPEE")

for i in range(5):
    app.columnconfigure(i, weight=1, minsize=100)
    app.rowconfigure(i, weight=1, minsize=50)

database_label = tk.Label(app, text="Database:")
database_label.grid(column=0, row=0, sticky="e", padx=5, pady=5)

database_path_var = tk.StringVar()
database_entry = tk.Entry(app, textvariable=database_path_var, width=40)
database_entry.grid(column=1, row=0, columnspan=2, sticky="ew", padx=5, pady=5)

browse_button = tk.Button(app, text="Browse", command=browse_directory)
browse_button.grid(column=3, row=0, sticky="w", padx=5, pady=5)

update_button = tk.Button(app, text="Update", command=update_database)
update_button.grid(column=4, row=0, sticky="w", padx=5, pady=5)

upload_label = tk.Label(app, text="Upload:")
upload_label.grid(column=0, row=1, sticky="e", padx=5, pady=5)

upload_path_var = tk.StringVar()
upload_entry = tk.Entry(app, textvariable=upload_path_var, width=40)
upload_entry.grid(column=1, row=1, columnspan=2, sticky="ew", padx=5, pady=5)

browse_upload_button = tk.Button(app, text="Browse", command=browse_upload_directory)
browse_upload_button.grid(column=3, row=1, sticky="w", padx=5, pady=5)

search_mode_label = tk.Label(app, text="Search mode:")
search_mode_label.grid(column=0, row=2, sticky="e", padx=5, pady=5)

search_mode_var = tk.StringVar()
search_mode_combobox = ttk.Combobox(app, textvariable=search_mode_var, values=("FAST", "TOP_ALIGNED", "ALL_ALIGNED"))
search_mode_combobox.grid(column=1, row=2, columnspan=2, sticky="ew", padx=5, pady=5)

search_type_label = tk.Label(app, text="Search type:")
search_type_label.grid(column=0, row=3, sticky="e", padx=5, pady=5)

search_type_var = tk.StringVar()
search_type_combobox = ttk.Combobox(app, textvariable=search_type_var, values=("FULL_LENGTH", "CONTAINED_IN", "CONTAINS", "RMSD", "Q_SCORE", "SSAP_SCORE"))
search_type_combobox.grid(column=1, row=3, columnspan=2, sticky="ew", padx=5, pady=5)

output_label = tk.Label(app, text="Output:")
output_label.grid(column=0, row=4, sticky="e", padx=5, pady=5)

output_path_var = tk.StringVar()
output_entry = tk.Entry(app, textvariable=output_path_var, width=40)
output_entry.grid(column=1, row=4, columnspan=2, sticky="ew", padx=5, pady=5)

browse_output_button = tk.Button(app, text="Browse", command=browse_output_directory)
browse_output_button.grid(column=3, row=4, sticky="w", padx=5, pady=5)

submit_button = tk.Button(app, text="Search", command=submit_form)
submit_button.grid(column=1, row=5, columnspan=2, padx=5, pady=5)

app.mainloop()
