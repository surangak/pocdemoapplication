<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>POC Demo</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    <style>
        div.padding {
            padding-top: 15px;
            padding-right: 15px;
            padding-bottom: 15px;
            padding-left: 15px;
        }
    </style>
</head>
<body>

<div class="container">
    <h2>POC Demo Application</h2>
    <ul class="nav nav-tabs">
        <li class="active"><a data-toggle="tab" href="#home">Save APHP Document</a></li>
        <li><a data-toggle="tab" href="#menu1">Save Patient</a></li>
        <li><a data-toggle="tab" href="#menu2">Query Clinical Data</a></li>
        <li><a data-toggle="tab" href="#menu3">Menu 3</a></li>
    </ul>

    <div class="tab-content">
        <div id="home" class="tab-pane fade in active">
            <h3>Enter APHP Document</h3>
            <div style="width:600px;height:150px;border:1px solid green;" class="padding">

                <form:form method="POST" commandName="form">
                    <table cellpadding="10">
                        <tr><td></td></tr>
                        <tr>
                            <td>Patient </td>
                            <td><form:select path="patient">
                                <form:option value="" label="...." />
                                <form:options items="${patients}" />
                            </form:select>
                            </td>
                            <td><form:errors path="patient" cssStyle="color: #ff0000;" /></td>

                        </tr>

                        <tr>
                            <td>Provider </td>
                            <td><form:select path="provider">
                                <form:option value="" label="...." />
                                <form:options items="${providers}" />
                            </form:select>
                            </td>
                            <td><form:errors path="provider" cssStyle="color: #ff0000;" /></td>

                        </tr>

                        <tr>
                            <form:hidden path="hiddenMessage"/>
                        </tr>
                        <tr><td></td></tr>
                        <tr>
                            <td><input type="submit" name="action" value="submit"></td>
                        </tr>
                        <tr>
                    </table>
                    <input type="hidden" name="message" value="form1" />
                </form:form></div>



        </div>
        <div id="menu1" class="tab-pane fade">
            <h3>Create Patient</h3>
            <div style="width:600px;height:150px;border:1px solid green;" class="padding">
                <form:form method="POST" commandName="patient">
                    <table cellpadding="10">
                        <tr><td></td></tr>
                        <tr><td>Identifier </td><td><input type="text" name="firstName"></td></tr>
                        <tr><td>First Name </td><td><input type="text" name="firstName"></td></tr>
                        <tr><td>Middle Name </td><td><input type="text" name="mname"></td></tr>
                        <tr><td>Last Name </td><td><input type="text" name="lastName"></td></tr>
                        <tr>
                            <td><input type="submit" name="action" value="patient"></td>
                        </tr>
                    </table>
                    <input type="hidden" name="message" value="form2" />
                </form:form>
            </div>
        </div>
        <div id="menu2" class="tab-pane fade">
            <h3>Query Clinical Data By Patient</h3>
            <div style="width:600px;height:150px;border:1px solid green;" class="padding">

            <form:form method="POST" commandName="patient">
                    <table cellpadding="10">
                        <tr><td></td></tr>
                        <tr><td>Patient Identifier </td><td><input type="text" name="identifier"></td></tr>
                        <tr><td>Identifier Type </td><td><input type="text" name="type"></td></tr>
                        <tr>
                            <td><input type="submit" name="action" value="query"></td>
                        </tr>
                        <tr>
                    </table>
                    <input type="hidden" name="message" value="form1" />
            </form:form>
                </div>
        </div>
    </div>
</div>

</body>
</html>
