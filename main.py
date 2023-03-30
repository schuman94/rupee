import subprocess
import tkinter as tk
from tkinter import ttk, messagebox

def run_rupee(search_mode, search_type):
    script_path = "./rupee-search/target/run_rupee-search.sh"
    subprocess.run([script_path, search_mode, search_type])
    messagebox.showinfo("Rupee", "La búsqueda ha finalizado.")

def submit_form():
    #query = query_entry.get()
    search_mode = search_mode_var.get()
    search_type = search_type_var.get()

    if not search_mode or not search_type:
        messagebox.showerror("Error", "Por favor, completa todos los campos.")
        return

    run_rupee(search_mode, search_type)

app = tk.Tk()
app.title("Rupee GUI")

query_entry = tk.Entry(app)
query_entry.grid(column=1, row=0)

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
