import { IdSupplier } from "src/common/interfaces";
import { RootFormInput, useRootObject } from "./RootObjectForm";
import hash from "object-hash"
import { useState } from "react";

export interface MultiSelectOption extends IdSupplier {
  name: string;
}

export interface MultiSelectProps<T extends MultiSelectOption> extends RootFormInput {
  options: T[];
  columns: string[];
}

export default function MultiSelect<T extends MultiSelectOption>(props: MultiSelectProps<T>) {
  const [rootObjectCon, rootObjectSetterCon] = useRootObject();
  const rootObject = props.rootObject ?? rootObjectCon;
  const rootObjectSetter = props.rootObjectSetter ?? rootObjectSetterCon;
  const [selectedOption, setSelectedOption] = useState<T | undefined>(undefined);
  const arrField = rootObject[props.field]

  return (
    <>
      <select
        value={selectedOption?.name}
        onChange={(s) => {
        }}
        className="form-control">
        {props.options.map(o => <option value={o.id}>{o.name}</option>)}
      </select>
      <button
        className="btn btn-secondary mt-2"
        onClick={() => {

        }}
      >Добавить</button>
      {arrField && arrField.length &&
        <table>
          <thead>
            {props.columns.map((c, i) => <th key={i}>{c}</th>)}
          </thead>
          <tbody>
            {arrField.map((i: any) =>
              <tr key={hash(i)}>
                {Object.values(i).map((v: any, index) => <td key={hash(i) + "." + index}>{v}</td>)}
              </tr>)}
          </tbody>
        </table>
      }
    </>
  );
}
