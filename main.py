import subprocess
import tkinter as tk
from tkinter import ttk, messagebox, filedialog

def run_rupee(search_mode, search_type):
    script_path = "./rupee-search/target/run_rupee-search.sh"
    subprocess.run([script_path, search_mode, search_type])
    messagebox.showinfo("Rupee", "La búsqueda ha finalizado.")

def update_database(database_path):
    database_path = database_path

    if not database_path:
        messagebox.showerror("Error", "Por favor, selecciona un directorio.")
        return

    constants_file_path = "src/main/java/edu/umkc/rupee/search/lib/Constants.java"
    new_dir_path_line = f'    public static String DIR_PATH = "{database_path}";\n'

    with open(constants_file_path, "r") as file:
        lines = file.readlines()

    with open(constants_file_path, "w") as file:
        for line in lines:
            if line.strip().startswith("public static String DIR_PATH"):
                file.write(new_dir_path_line)
            else:
                file.write(line)

    script_path = "./actualizar_bbdd.sh"
    subprocess.run([script_path])
    messagebox.showinfo("Rupee", "La base de datos se ha actualizado.")


def submit_form():
    search_mode = search_mode_var.get()
    search_type = search_type_var.get()

    if not search_mode or not search_type:
        messagebox.showerror("Error", "Por favor, completa todos los campos.")
        return

    run_rupee(search_mode, search_type)

def browse_directory():
    directory = filedialog.askdirectory()
    database_path_var.set(directory)

def update_database_button():
    database_path = database_path_var.get()
    if not database_path:
        messagebox.showerror("Error", "Por favor, selecciona un directorio.")
        return

    update_database(database_path)

app = tk.Tk()
app.title("Rupee GUI")
#app.geometry("600x300")

database_label = tk.Label(app, text="Base de datos:")
database_label.grid(column=0, row=0)

database_path_var = tk.StringVar()
database_entry = tk.Entry(app, textvariable=database_path_var)
database_entry.grid(column=1, row=0)

browse_button = tk.Button(app, text="Examinar", command=browse_directory)
browse_button.grid(column=2, row=0)

update_button = tk.Button(app, text="Actualizar", command=update_database_button)
update_button.grid(column=3, row=0)

search_mode_label = tk.Label(app, text="SEARCH_MODE:")
search_mode_label.grid(column=0, row=1)

search_mode_var = tk.StringVar()
search_mode_combobox = ttk.Combobox(app, textvariable=search_mode_var, values=("FAST", "TOP_ALIGNED", "ALL_ALIGNED"))
search_mode_combobox.grid(column=1, row=1)

search_type_label = tk.Label(app, text="SEARCH_TYPE:")
search_type_label.grid(column=0, row=2)

search_type_var = tk.StringVar()
search_type_combobox = ttk.Combobox(app, textvariable=search_type_var, values=("FULL_LENGTH", "CONTAINED_IN", "CONTAINS", "RMSD", "Q_SCORE", "SSAP_SCORE"))
search_type_combobox.grid(column=1, row=2)

submit_button = tk.Button(app, text="Ejecutar", command=submit_form)
submit_button.grid(column=1, row=3)

app.mainloop()
