<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Captura de Inventario</title>
    <script>
        function agregarProducto() {
            const productName = document.getElementById('productName').value;
            const quantity = document.getElementById('quantity').value;
            const category = document.getElementById('category').value;

            const listItem = document.createElement('li');
            listItem.textContent = `Producto: ${productName}, Cantidad: ${quantity}, Categoría: ${category}`;
            document.getElementById('inventoryList').appendChild(listItem);

            document.getElementById('productForm').reset();
        }
    </script>
</head>
<body>
    <h1>Captura de Inventario de Productos</h1>
    <form id="productForm">
        <label for="productName">Nombre del Producto:</label>
        <input type="text" id="productName" required><br><br>

        <label for="quantity">Cantidad:</label>
        <input type="number" id="quantity" required><br><br>

        <label for="category">Categoría:</label>
        <select id="category" required>
            <option value="">Seleccione una categoría</option>
            <option value="electronica">Electrónica</option>
            <option value="alimentos">Alimentos</option>
            <option value="ropa">Ropa</option>
        </select><br><br>

        <button type="button" onclick="agregarProducto()">Agregar Producto</button>
    </form>

    <h2>Inventario Actual</h2>
    <ul id="inventoryList"></ul>
</body>
</html>
