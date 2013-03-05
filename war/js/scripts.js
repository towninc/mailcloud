
$.replacerLabel = function(emailText, size){
    var labels = null;
    for(var i = 0; i < size; i++){
        if(i == 0){
            labels = '<th>'
            + '$' + String.fromCharCode(65 + i)
            + ' ('
            + emailText
            + ')'
            + '<\/th>';
        } else {
            labels += '<th>'
            + '$' + String.fromCharCode(65 + i)
            +'<\/th>';
        }
    }
    return labels;
}

$.replacerValues = function(data, size){
    var values = null;
    var dataSize = data.length;
    if(dataSize == 0){
        return null;
    }
    
    for(var i = 0; i < size; i++){
        if(i == 0){
            values = '<td>' + $.escapeHTML(data[i]) + '<\/td>';
        } else {
            if(i >= dataSize){
                values += '<td><\/td>';
            } else {
                values += '<td>' + $.escapeHTML(data[i]) + '<\/td>';
            }
        }
    }
    return values;
}

$.showFormTitle = function(formId) {
    var id = $("#"+formId).find("input[name=id]").val();
    if(id){
        $("#editTitle").show();
    } else {
        $("#addTitle").show();
    }
}
