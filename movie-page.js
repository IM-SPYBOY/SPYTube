document.addEventListener("DOMContentLoaded", () => {
  // Download section toggle functionality
  const downloadHeaders = document.querySelectorAll(".download-header")

  downloadHeaders.forEach((header) => {
    header.addEventListener("click", () => {
      const fileId = header.getAttribute("data-file-id")
      const content = document.getElementById(`content-${fileId}`)
      const chevron = header.querySelector(".chevron-icon")

      if (content) {
        content.classList.toggle("hidden")

        if (chevron) {
          chevron.style.transform = content.classList.contains("hidden") ? "rotate(0deg)" : "rotate(180deg)"
        }
      }
    })
  })

  // Trailer modal functionality
  const trailerBtn = document.getElementById("trailer-btn")
  const trailerModal = document.getElementById("trailer-modal")
  const closeTrailer = document.getElementById("close-trailer")
  const trailerContainer = document.getElementById("trailer-container")
  let trailerIframe = null

  if (trailerBtn && trailerModal && trailerContainer) {
    trailerBtn.addEventListener("click", () => {
      trailerModal.classList.remove("hidden")

      // Get trailer URL from data attribute
      const trailerUrl = trailerBtn.getAttribute("data-trailer-url")

      // Create iframe if it doesn't exist
      if (!trailerIframe) {
        
        trailerIframe = document.createElement("iframe")
        trailerIframe.width = "100%"
        trailerIframe.height = "100%"
        trailerIframe.src = trailerUrl
        trailerIframe.id = "youtube-player";
        trailerIframe.allow = "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        trailerIframe.allowFullscreen = true
        trailerIframe.classList.add("rounded-md")

        // Insert iframe into container
        trailerContainer.appendChild(trailerIframe)
      }

      // Prevent body scrolling
      document.body.style.overflow = "hidden"
    })

    if (closeTrailer) {
      closeTrailer.addEventListener("click", () => {
        trailerModal.classList.add("hidden")
        // Restore body scrolling
        if(trailerIframe){
            trailerIframe.contentWindow.postMessage(
              JSON.stringify({
                event: 'command',
                func: 'pauseVideo',
                args: []
              }),
              '*'
            );
        }
        document.body.style.overflow = ""
      })
    }

    // Close modal when clicking outside
    trailerModal.addEventListener("click", (e) => {
      if (e.target === trailerModal) {
        trailerModal.classList.add("hidden")
        // Restore body scrolling
        document.body.style.overflow = ""
      }
    })

    // Close modal with Escape key
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && !trailerModal.classList.contains("hidden")) {
        trailerModal.classList.add("hidden")
        // Restore body scrolling
        document.body.style.overflow = ""
      }
    })
  }

})

