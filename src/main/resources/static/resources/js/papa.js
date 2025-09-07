$(function(){
    $("form").each(function(){
        const modalDiv = $(this).find("div.overlayModal")[0];
        if(modalDiv != null){
            console.log("found overlay modal id "+modalDiv.id);
            $(this).on("keypress", function (event){
                //ENTER pressed
                if(event.keyCode === 13){
                    event.preventDefault();
                    //make url to focus overlay modal
                    let new_href = window.location.toString().substring(0, window.location.toString().lastIndexOf('#'))
                                    + "#" + modalDiv.id;
                    window.location.replace(new_href);
                }
            })
        }
    })
});