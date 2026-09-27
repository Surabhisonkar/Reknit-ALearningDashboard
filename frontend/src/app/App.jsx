import { Routes, Route } from "react-router-dom";

import Layout from "./Layout";
import { APP_ROUTES } from "./routes.jsx";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {APP_ROUTES.map(({ path, element }) => (
          <Route key={path} path={path} element={element} />
        ))}
      </Route>
    </Routes>
  );
}
