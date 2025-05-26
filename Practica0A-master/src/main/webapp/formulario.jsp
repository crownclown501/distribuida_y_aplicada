<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Formulario de Validación</title>
    <style>
        .form-group {
            margin-bottom: 15px;
        }
        .form-group label {
            display: block;
            margin-bottom: 5px;
        }
        .form-group input, .form-group select {
            width: 100%;
            padding: 8px;
            box-sizing: border-box;
        }
        .form-group input[type="checkbox"] {
            width: auto;
        }
        .guide {
            color: #666;
            font-size: 0.9em;
        }
    </style>
    <script>
        function validarFormulario() {
            // Validar NIF
            const nif = document.getElementById('nif').value;
            const nifRegex = /^\d{8}[A-Za-z]$/;
            if (!nifRegex.test(nif)) {
                alert('El NIF debe tener 8 dígitos seguidos de una letra.');
                return false;
            }

            // Validar correo electrónico
            const email = document.getElementById('email').value;
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                alert('El correo electrónico no es válido.');
                return false;
            }

            // Validar claves de usuario
            const password1 = document.getElementById('password1').value;
            const password2 = document.getElementById('password2').value;
            if (password1 !== password2) {
                alert('Las claves de usuario no coinciden.');
                return false;
            }

            // Validar pregunta con opciones
            const options = document.querySelectorAll('input[name="option"]:checked');
            if (options.length === 0) {
                alert('Debe seleccionar al menos una opción.');
                return false;
            }

            alert('Formulario enviado correctamente.');
            return true;
        }
    </script>
</head>
<body>
    <h1>Formulario de Validación</h1>
    <form id="formulario" onsubmit="return validarFormulario()">
        <div class="form-group">
            <label for="nif">NIF (Ejemplo: 12345678A):</label>
            <input type="text" id="nif" name="nif" required>
            <span class="guide">El NIF debe tener 8 dígitos seguidos de una letra.</span>
        </div>

        <div class="form-group">
            <label for="email">Correo Electrónico:</label>
            <input type="email" id="email" name="email" required>
        </div>

        <div class="form-group">
            <label for="password1">Clave de Usuario:</label>
            <input type="password" id="password1" name="password1" required>
        </div>

        <div class="form-group">
            <label for="password2">Confirmar Clave:</label>
            <input type="password" id="password2" name="password2" required>
        </div>

        <div class="form-group">
            <label>Opciones:</label><br>
            <input type="checkbox" name="option" value="opcion1" id="option1">
            <label for="option1">Opción 1 - Recibir notificaciones por correo electrónico.</label><br>
            <input type="checkbox" name="option" value="opcion2" id="option2">
            <label for="option2">Opción 2 - Aceptar términos y condiciones.</label>
        </div>

        <button type="submit">Enviar</button>
    </form>
</body>
</html>
