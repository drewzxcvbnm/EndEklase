import { ReactElement, useEffect, useState } from "react";
import { DeleteIcon, EditIcon } from "src/assets/Icons";
import { IdSupplier } from "src/common/interfaces";
import Modal from "src/common/Modal";

/* eslint-disable @typescript-eslint/no-explicit-any */
interface CrudTableProps<T extends IdSupplier> {
  items: T[];
  columns?: string[];
  excludeColumns?: string[];
  onDelete: (item: T) => void;
  editFormSupplier: (item: T, closeForm: () => void, isOpen: boolean) => ReactElement;
}

interface CrudRowProps<T extends IdSupplier> {
  item: T;
  deleteItem: () => void;
  editFormSupplier: (item: T, closeForm: () => void, isOpen: boolean) => ReactElement;
}

function ActionButton(props: any) {
  return (
    <button onClick={props.onClick} className="bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:outline-none focus:ring-blue-300 font-medium rounded-lg text-sm px-2.5 py-2.5 text-center inline-flex items-center me-2 dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800">
      {props.children}
    </button>
  );
}

function CrudTableRow<T extends IdSupplier>(props: CrudRowProps<T>) {
  const [item, setItem] = useState<T>(props.item);
  useEffect(() => setItem(props.item), [props.item]);
  const [openModal, setOpenModal] = useState(false);
  return (
    <>
      <tr key={props.item.id} className="bg-white border-b dark:bg-gray-800 dark:border-gray-700">
        {Object.values(props.item).map((v: any, index) => {
          return <td key={props.item.id + '.' + index} className="px-6 py-4 font-medium text-gray-900 whitespace-nowrap dark:text-white">{v}</td>;
        })}
        <td key={props.item.id + '.' + Object.values(props.item).length}>
          <Modal isOpen={openModal}>
            {props.editFormSupplier(item, () => {
              setOpenModal(false)
              setItem(item);
            }, openModal)}
          </Modal>
          <div className="flex align-center justify-center">
            <ActionButton onClick={() => {
              setOpenModal(true);
            }}>
              <EditIcon />
            </ActionButton>
            <ActionButton onClick={props.deleteItem}>
              <DeleteIcon />
            </ActionButton>
          </div>
        </td>
      </tr>
    </>
  );
}

export default function CrudTable<T extends IdSupplier>(props: CrudTableProps<T>) {
  const [items, setItems] = useState(props.items);
  useEffect(() => setItems(props.items), [props.items])
  let columns = props.columns || (items.length ? Object.keys(items[0]) : []);
  if (props.excludeColumns) {
    columns = columns.filter(c => !props.excludeColumns?.includes(c));
  }

  if (!columns.length) {
    return (<>
      <div className="flex align-center justify-center mt-5 text-3xl">Empty table</div>
    </>);
  }
  return (<>
    <table className="table-bordered mt-5 w-full text-sm text-left rtl:text-right text-gray-500 dark:text-gray-400">
      <thead className="text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400">
        <tr>
          {columns.map((c, index) => <th key={index} scope="col" className="px-6 py-3">{c}</th>)}
          <th scope="col" className="px-6 py-3">Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((it) => <CrudTableRow
          key={it.id}
          editFormSupplier={props.editFormSupplier}
          item={it}
          deleteItem={() => {
            props.onDelete(it);
            setItems(items.filter(i => i != it))
          }}
        />)}
      </tbody>
    </table>
  </>);
}
