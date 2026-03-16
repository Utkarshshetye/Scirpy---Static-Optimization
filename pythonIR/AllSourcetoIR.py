import ast
from astexport.export import export_json
import os
def main():

    ex_path="/home/bhushan/intellijprojects/scirpy/pythonIR/testsuite"
    i=1
    for root, dirs, files in os.walk(ex_path):
        for name in files:
            print(name)
            testfile=os.path.join(ex_path,name)
            with open(testfile, "r") as source:
                    tree = ast.parse(source.read())
                    f= open("/home/bhushan/intellijprojects/scirpy/irs/ex"+str(i)+".json","w+")
                    print(f)
                    json = export_json(tree, "True")
                    f.write(json)
                    f.close()
                    i=i+1

if __name__ == "__main__":
    main()
