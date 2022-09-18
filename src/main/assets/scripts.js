function setSongName(songName){
    var context = document.getElementById("song-name");
    context.innerHTML = songName;
}

function setArtistName(artistName){
    var context = document.getElementById("artist-name");
    context.innerHTML = artistName;
}


function processing(data){
    var lyrics_content =document.getElementById("lyrics-content");
    if(data.author == ""){ data.author = "Unknown"; }
    setSongName(data.song);
    setArtistName(data.author);
    var html = "";
    for(var i=0;i<data.lyrics.length;i++){
        html = html + "<h2>"+data.lyrics[i]+"</h2>";
        }
    lyrics_content.innerHTML = html;
}

function loadSong(data){
    processing(data);
  }

function highlight_sentence(x){
    if (x == 0) {
        var element = document.getElementById("song-name")
        element.classList.add("current");

    }
    if (x == 1) {
        var element = document.getElementById("artist-name")
        element.classList.add("current");
    }
    if (x > 1) {
        var element = (document.getElementById("lyrics-content").children)[x-2];
        element.classList.add("current");
    }

    element.scrollIntoView({behavior: "smooth", inline: "center", block: "center"});

}

function remove_highlight(x){
    if (x == 0) {
        document.getElementById("song-name").classList.remove("current");
    }
    if (x == 1) {
        document.getElementById("artist-name").classList.remove("current");
    }
    if (x > 1) {
        var sentences = document.getElementById("lyrics-content").children;
        sentences[x-2].classList.remove("current");
    }
}